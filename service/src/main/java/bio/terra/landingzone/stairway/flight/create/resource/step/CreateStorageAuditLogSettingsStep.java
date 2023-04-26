package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateStorageAuditLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateStorageAuditLogSettingsStep.class);

  public CreateStorageAuditLogSettingsStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var storageAccountId =
        getParameterOrThrow(
            context.getWorkingMap(), CreateStorageAccountStep.STORAGE_ACCOUNT_ID, String.class);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);

    var storageAuditLogSettingsName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH);

    var storageAuditLogSettings =
        armManagers
            .monitorManager()
            .diagnosticSettings()
            .define(storageAuditLogSettingsName)
            .withResource(storageAccountId + "/blobServices/default")
            .withLogAnalytics(logAnalyticsWorkspaceId)
            .withLog("StorageRead", 0) // retention is handled by the log analytics workspace
            .withLog("StorageWrite", 0)
            .withLog("StorageDelete", 0)
            .create();
    logger.info(
        RESOURCE_CREATED, getResourceType(), storageAuditLogSettings.id(), getMRGName(context));
  }

  @Override
  protected String getResourceType() {
    return "StorageAuditLogSettings";
  }
}
