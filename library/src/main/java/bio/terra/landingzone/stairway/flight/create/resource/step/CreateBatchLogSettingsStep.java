package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.monitor.models.DiagnosticSettingsCategory;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateBatchLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateBatchLogSettingsStep.class);

  public CreateBatchLogSettingsStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    // will be removed as part of batch account deletion
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

    var batchLogSettingsName = resourceNameProvider.getName(getResourceType());

    for (DiagnosticSettingsCategory diagnosticSettingsCategory :
        armManagers
            .monitorManager()
            .diagnosticSettings()
            .listCategoriesByResource(batchAccountId)) {
      logger.info(
          "Currently valid diagnostic settings category for batch in current azure Environment :"
              + diagnosticSettingsCategory.name());
    }

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
    logger.info(RESOURCE_CREATED, getResourceType(), batchLogSettings.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    // do nothing
  }

  @Override
  protected String getResourceType() {
    return "BatchLogSettings";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.empty();
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH));
  }
}
