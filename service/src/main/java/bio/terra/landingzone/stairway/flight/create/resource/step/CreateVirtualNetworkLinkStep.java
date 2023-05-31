package bio.terra.landingzone.stairway.flight.create.resource.step;

import static bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator.MAX_PRIVATE_VNET_LINK_NAME_LENGTH;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.SubResource;
import com.azure.resourcemanager.privatedns.fluent.models.VirtualNetworkLinkInner;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateVirtualNetworkLinkStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateVirtualNetworkLinkStep.class);
  public static final String VNET_LINK_ID = "VNET_LINK_ID";
  public static final String VNET_LINK_RESOURCE_KEY = "VNET_LINK";

  public CreateVirtualNetworkLinkStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var vNetId = getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);
    var dns =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreatePostgresqlDNSStep.POSTGRESQL_DNS_RESOURCE_KEY,
            LandingZoneResource.class);

    var vnetLink =
        armManagers
            .azureResourceManager()
            .privateDnsZones()
            .manager()
            .serviceClient()
            .getVirtualNetworkLinks()
            .createOrUpdate(
                getMRGName(context),
                dns.resourceName().orElseThrow(),
                resourceNameGenerator.nextName(MAX_PRIVATE_VNET_LINK_NAME_LENGTH),
                new VirtualNetworkLinkInner()
                    .withLocation("global")
                    .withTags(
                        Map.of(
                            LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                            landingZoneId.toString()))
                    .withVirtualNetwork(new SubResource().withId(vNetId))
                    .withRegistrationEnabled(false));

    context.getWorkingMap().put(VNET_LINK_ID, vnetLink.id());
    context
        .getWorkingMap()
        .put(
            VNET_LINK_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(vnetLink.id())
                .resourceType(vnetLink.type())
                .tags(vnetLink.tags())
                .resourceName(vnetLink.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), vnetLink.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    var idParts = resourceId.split("/");
    if (idParts.length != 11) {
      throw new IllegalArgumentException(
          String.format("Invalid vnet link resourceId: %s", resourceId));
    }
    var mrgName = idParts[4];
    var privateDnsZoneName = idParts[8];
    var vnetLinkName = idParts[10];

    armManagers
        .azureResourceManager()
        .privateDnsZones()
        .manager()
        .serviceClient()
        .getVirtualNetworkLinks()
        .delete(mrgName, privateDnsZoneName, vnetLinkName);
  }

  @Override
  protected String getResourceType() {
    return "PrivateDnsZone";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(VNET_LINK_ID, String.class));
  }
}
