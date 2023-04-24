package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateBatchLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateBatchLogSettingsStep.class);

  public CreateBatchLogSettingsStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
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

    var batchLogSettings =
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
    logger.info(RESOURCE_CREATED, getResourceType(), batchLogSettings.id(), resourceGroup.name());
  }

  @Override
  protected String getResourceType() {
    return "BatchLogSettings";
  }
}
