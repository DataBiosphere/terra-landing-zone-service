package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDiagnosticSetting;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateStorageAuditLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateStorageAuditLogSettingsStep.class);

  public static final String STORAGE_AUDIT_LOG_SETTINGS_KEY = "STORAGE_AUDIT_LOG_SETTINGS";

  public CreateStorageAuditLogSettingsStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider) {
    super(armManagers, parametersResolver, resourceNameProvider);
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

    var storageAuditLogSettingsName = resourceNameProvider.getName(getResourceType());

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

    context
        .getWorkingMap()
        .put(
            STORAGE_AUDIT_LOG_SETTINGS_KEY,
            new LandingZoneDiagnosticSetting(
                storageAuditLogSettings.resourceId(),
                storageAuditLogSettingsName,
                storageAuditLogSettings.logs()));
    logger.info(
        RESOURCE_CREATED, getResourceType(), storageAuditLogSettings.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    // do nothing
  }

  @Override
  protected String getResourceType() {
    return "StorageAuditLogSettings";
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
