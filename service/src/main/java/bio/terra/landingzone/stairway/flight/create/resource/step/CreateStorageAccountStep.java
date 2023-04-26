package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateStorageAccountStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateStorageAccountStep.class);
  public static final String STORAGE_ACCOUNT_ID = "STORAGE_ACCOUNT_ID";
  public static final String STORAGE_ACCOUNT_RESOURCE_KEY = "STORAGE_ACCOUNT";

  public CreateStorageAccountStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // rollback here or in case of sub-flight do it there
    var storageAccountId = context.getWorkingMap().get(STORAGE_ACCOUNT_ID, String.class);
    try {
      if (storageAccountId != null) {
        armManagers.azureResourceManager().storageAccounts().deleteById(storageAccountId);
      }
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error(
            "Storage account doesn't exist or has been already deleted. Id={}", storageAccountId);
        return StepResult.getStepResultSuccess();
      }
      logger.error("Failed attempt to delete storage account. Id={}", storageAccountId);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    String storageAccountName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_STORAGE_ACCOUNT_NAME_LENGTH);
    var storage =
        armManagers
            .azureResourceManager()
            .storageAccounts()
            .define(storageAccountName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
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
  protected String getResourceType() {
    return "StorageAccount";
  }
}
