package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.storage.models.SkuName;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateStorageAccountStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateStorageAccountStep.class);
  public static final String STORAGE_ACCOUNT_ID = "STORAGE_ACCOUNT_ID";
  public static final String STORAGE_ACCOUNT_RESOURCE_KEY = "STORAGE_ACCOUNT";

  public CreateStorageAccountStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    String storageAccountName = resourceNameProvider.getName(getResourceType());
    var storage =
        armManagers
            .azureResourceManager()
            .storageAccounts()
            .define(storageAccountName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withSku(
                StorageAccountSkuType.fromSkuName(
                    SkuName.fromString(
                        getParametersResolver(context)
                            .getValue(
                                LandingZoneDefaultParameters.ParametersNames
                                    .STORAGE_ACCOUNT_SKU_TYPE
                                    .name()))))
            .disableBlobPublicAccess()
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();

    context.getWorkingMap().put(STORAGE_ACCOUNT_ID, storage.id());
    context.getWorkingMap().put(LandingZoneFlightMapKeys.STORAGE_ACCOUNT_NAME, storageAccountName);
    context
        .getWorkingMap()
        .put(
            STORAGE_ACCOUNT_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(storage.id())
                .resourceType(storage.type())
                .tags(storage.tags())
                .region(storage.regionName())
                .resourceName(storage.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), storage.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().storageAccounts().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "StorageAccount";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(STORAGE_ACCOUNT_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_STORAGE_ACCOUNT_NAME_LENGTH));
  }
}
