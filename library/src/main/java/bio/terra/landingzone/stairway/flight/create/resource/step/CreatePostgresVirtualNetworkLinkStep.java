package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.common.VirtualNetworkLinkResourceHelper;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.SubResource;
import com.azure.resourcemanager.privatedns.fluent.models.VirtualNetworkLinkInner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePostgresVirtualNetworkLinkStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreatePostgresVirtualNetworkLinkStep.class);
  public static final String POSTGRES_VNET_LINK_ID = "POSTGRES_VNET_LINK_ID";
  public static final String POSTGRES_VNET_LINK_RESOURCE_KEY = "POSTGRES_VNET_LINK";

  public CreatePostgresVirtualNetworkLinkStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
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
            CreatePostgresqlDNSZoneStep.POSTGRESQL_DNS_RESOURCE_KEY,
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
                resourceNameProvider.getName(getResourceType()),
                new VirtualNetworkLinkInner()
                    .withLocation("global")
                    .withTags(
                        Map.of(
                            LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                            landingZoneId.toString()))
                    .withVirtualNetwork(new SubResource().withId(vNetId))
                    .withRegistrationEnabled(false));

    context.getWorkingMap().put(POSTGRES_VNET_LINK_ID, vnetLink.id());
    context
        .getWorkingMap()
        .put(
            POSTGRES_VNET_LINK_RESOURCE_KEY,
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
    VirtualNetworkLinkResourceHelper.delete(armManagers, resourceId);
  }

  @Override
  protected String getResourceType() {
    return "PostgresVirtualNetworkLink";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(POSTGRES_VNET_LINK_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_PRIVATE_VNET_LINK_NAME_LENGTH));
  }
}
