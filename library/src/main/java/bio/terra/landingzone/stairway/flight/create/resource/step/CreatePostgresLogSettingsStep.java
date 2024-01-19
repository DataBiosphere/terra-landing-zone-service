package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePostgresLogSettingsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreatePostgresLogSettingsStep.class);

  public CreatePostgresLogSettingsStep(
      ArmManagers armManagers,
      bio.terra.landingzone.stairway.flight.ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context) {
    var postgreSqlId =
        getParameterOrThrow(
            context.getWorkingMap(), CreatePostgresqlDbStep.POSTGRESQL_ID, String.class);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);

    var postgresLogSettingsName = resourceNameProvider.getName(getResourceType());

    var postgresLogSettings =
        armManagers
            .monitorManager()
            .diagnosticSettings()
            .define(postgresLogSettingsName)
            .withResource(postgreSqlId)
            .withLogAnalytics(logAnalyticsWorkspaceId)
            .withLog("PostgreSQLLogs", 0) // retention is handled by the log analytics workspace
            .withLog("PostgreSQLFlexQueryStoreRuntime", 0)
            .withLog("PostgreSQLFlexQueryStoreWaitStats", 0)
            .withMetric("AllMetrics", Duration.ofMinutes(1), 0)
            .create();
    logger.info(RESOURCE_CREATED, getResourceType(), postgresLogSettings.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    // do nothing
  }

  @Override
  protected String getResourceType() {
    return "PostgresLogSettings";
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
