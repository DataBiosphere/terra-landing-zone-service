package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateBatchAccountStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateBatchAccountStep.class);
  public static final String BATCH_ACCOUNT_ID = "BATCH_ACCOUNT_ID";
  public static final String BATCH_ACCOUNT_RESOURCE_KEY = "BATCH_ACCOUNT";

  public CreateBatchAccountStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    var batchAccountId = context.getWorkingMap().get(BATCH_ACCOUNT_ID, String.class);
    try {
      if (batchAccountId != null) {
        armManagers.batchManager().batchAccounts().deleteById(batchAccountId);
      }
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error(
            "Batch account doesn't exist or has been already deleted. Id={}", batchAccountId);
        return StepResult.getStepResultSuccess();
      }
      logger.error("Failed attempt to delete batch account. Id={}", batchAccountId);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    String batchAccountName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_BATCH_ACCOUNT_NAME_LENGTH);
    var batch =
        armManagers
            .batchManager()
            .batchAccounts()
            .define(batchAccountName)
            .withRegion(resourceGroup.region())
            .withExistingResourceGroup(resourceGroup.name())
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();
    context.getWorkingMap().put(BATCH_ACCOUNT_ID, batch.id());
    context
        .getWorkingMap()
        .put(
            BATCH_ACCOUNT_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(batch.id())
                .resourceType(batch.type())
                .tags(batch.tags())
                .region(batch.regionName())
                .resourceName(batch.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), batch.id(), resourceGroup.name());
  }

  @Override
  protected String getResourceType() {
    return "BatchAccount";
  }
}
