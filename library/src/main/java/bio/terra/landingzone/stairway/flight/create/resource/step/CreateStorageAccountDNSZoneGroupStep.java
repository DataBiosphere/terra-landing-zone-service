package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.network.fluent.models.PrivateDnsZoneGroupInner;
import com.azure.resourcemanager.network.models.PrivateDnsZoneConfig;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates a DNS Zone Group, which is a link between a private endpoint and a private DNS Zone. */
public class CreateStorageAccountDNSZoneGroupStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateStorageAccountDNSZoneGroupStep.class);
  public static final String STORAGE_ACCOUNT_DNS_ZONE_GROUP_ID =
      "STORAGE_ACCOUNT_DNS_ZONE_GROUP_ID";

  public CreateStorageAccountDNSZoneGroupStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var privateEndpoint =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateStorageAccountPrivateEndpointStep.STORAGE_ACCOUNT_PRIVATE_ENDPOINT_RESOURCE_KEY,
            LandingZoneResource.class);

    var privateDns =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateStorageAccountDNSZoneStep.STORAGE_ACCOUNT_DNS_RESOURCE_KEY,
            LandingZoneResource.class);

    var dnsZoneGroup =
        armManagers
            .azureResourceManager()
            .privateEndpoints()
            .manager()
            .serviceClient()
            .getPrivateDnsZoneGroups()
            .createOrUpdate(
                getMRGName(context),
                privateEndpoint.resourceName().orElseThrow(),
                resourceNameProvider.getName(getResourceType()),
                new PrivateDnsZoneGroupInner()
                    .withPrivateDnsZoneConfigs(
                        List.of(
                            new PrivateDnsZoneConfig()
                                .withPrivateDnsZoneId(privateDns.resourceId())
                                .withName(
                                    CreateStorageAccountDNSZoneStep.STORAGE_ACCOUNT_DNS_NAME
                                        .replace('.', '_')))));

    context.getWorkingMap().put(STORAGE_ACCOUNT_DNS_ZONE_GROUP_ID, dnsZoneGroup.id());

    logger.info(RESOURCE_CREATED, getResourceType(), dnsZoneGroup.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    // TODO delete helper
    //
    // armManagers.azureResourceManager().privateEndpoints().manager().serviceClient().getPrivateDnsZoneGroups().delete();
  }

  @Override
  protected String getResourceType() {
    return "StorageAccountPrivateDnsZoneGroup";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(
        context.getWorkingMap().get(STORAGE_ACCOUNT_DNS_ZONE_GROUP_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_PRIVATE_DNS_ZONE_GROUP_NAME_LENGTH));
  }
}
