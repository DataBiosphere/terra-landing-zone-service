package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates a private DNS Zone for the storage account. */
public class CreateStorageAccountDNSZoneStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateStorageAccountDNSZoneStep.class);
  public static final String STORAGE_ACCOUNT_DNS_ID = "STORAGE_ACCOUNT_DNS_ID";
  public static final String STORAGE_ACCOUNT_DNS_RESOURCE_KEY = "STORAGE_ACCOUNT_DNS";
  public static final String STORAGE_ACCOUNT_DNS_NAME = "privatelink.blob.core.windows.net";

  public CreateStorageAccountDNSZoneStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var dns =
        armManagers
            .azureResourceManager()
            .privateDnsZones()
            .define(STORAGE_ACCOUNT_DNS_NAME)
            .withExistingResourceGroup(getMRGName(context))
            .withTags(
                Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId.toString()))
            .create();

    context.getWorkingMap().put(STORAGE_ACCOUNT_DNS_ID, dns.id());
    context
        .getWorkingMap()
        .put(
            STORAGE_ACCOUNT_DNS_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(dns.id())
                .resourceType(dns.type())
                .tags(dns.tags())
                .region(dns.regionName())
                .resourceName(dns.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), dns.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().privateDnsZones().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "StorageAccountPrivateDnsZone";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(STORAGE_ACCOUNT_DNS_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_PRIVATE_DNS_ZONE_NAME_LENGTH));
  }
}
