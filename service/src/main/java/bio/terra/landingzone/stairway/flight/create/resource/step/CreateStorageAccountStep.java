package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateStorageAccountStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateStorageAccountStep.class);
  public static final String STORAGE_ACCOUNT_ID = "STORAGE_ACCOUNT_ID";
  public static final String STORAGE_ACCOUNT_RESOURCE_KEY = "STORAGE_ACCOUNT";

  public CreateStorageAccountStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);
    String storageAccountName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_STORAGE_ACCOUNT_NAME_LENGTH);
    try {
      var storage =
          armManagers
              .azureResourceManager()
              .storageAccounts()
              .define(storageAccountName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup)
              .withTags(
                  Map.of(
                      LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                      landingZoneId.toString(),
                      LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                      ResourcePurpose.SHARED_RESOURCE.toString()))
              .create();

      context.getWorkingMap().put(STORAGE_ACCOUNT_ID, storage.id());
      context
          .getWorkingMap()
          .put(LandingZoneFlightMapKeys.STORAGE_ACCOUNT_NAME, storageAccountName);
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
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS, "Storage account", storageAccountName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "storage account", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
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
}
