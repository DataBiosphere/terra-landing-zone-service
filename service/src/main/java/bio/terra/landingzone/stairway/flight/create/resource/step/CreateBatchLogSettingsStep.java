package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateBatchLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateBatchLogSettingsStep.class);

  public CreateBatchLogSettingsStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);

    var batchAccountId =
        getParameterOrThrow(
            context.getWorkingMap(), CreateBatchAccountStep.BATCH_ACCOUNT_ID, String.class);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);

    var batchLogSettingsName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH);
    try {
      armManagers
          .monitorManager()
          .diagnosticSettings()
          .define(batchLogSettingsName)
          .withResource(batchAccountId)
          .withLogAnalytics(logAnalyticsWorkspaceId)
          .withLog("ServiceLogs", 0) // retention is handled by the log analytics workspace
          .withLog("ServiceLog", 0)
          .withLog("AuditLog", 0)
          .create();
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS,
            "Batch account log settings",
            batchLogSettingsName,
            resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(
          FAILED_TO_CREATE_RESOURCE, "batch account log settings", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return StepResult.getStepResultSuccess();
  }
}
