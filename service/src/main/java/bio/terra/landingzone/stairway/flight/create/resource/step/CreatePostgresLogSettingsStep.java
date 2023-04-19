package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePostgresLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreatePostgresLogSettingsStep.class);

  public CreatePostgresLogSettingsStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);

    var postgreSqlId =
        getParameterOrThrow(
            context.getWorkingMap(), CreatePostgresqlDbStep.POSTGRESQL_ID, String.class);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);

    var postgresLogSettingsName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH);
    try {
      armManagers
          .monitorManager()
          .diagnosticSettings()
          .define(postgresLogSettingsName)
          .withResource(postgreSqlId)
          .withLogAnalytics(logAnalyticsWorkspaceId)
          .withLog("PostgreSQLLogs", 0) // retention is handled by the log analytics workspace
          .withLog("QueryStoreRuntimeStatistics", 0)
          .withLog("QueryStoreWaitStatistics", 0)
          .withMetric("AllMetrics", Duration.ofMinutes(1), 0)
          .create();
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS,
            "Postgres log settings",
            postgresLogSettingsName,
            resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "postgres log settings", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
