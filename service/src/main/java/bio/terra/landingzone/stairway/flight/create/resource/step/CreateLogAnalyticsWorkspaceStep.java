package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
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

public class CreateLogAnalyticsWorkspaceStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateLogAnalyticsWorkspaceStep.class);

  public static final String LOG_ANALYTICS_WORKSPACE_ID = "LOG_ANALYTICS_WORKSPACE_ID";
  public static final String LOG_ANALYTICS_RESOURCE_KEY = "LOG_ANALYTICS";

  public CreateLogAnalyticsWorkspaceStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    String logAnalyticsWorkspaceId =
        context.getWorkingMap().get(LOG_ANALYTICS_WORKSPACE_ID, String.class);
    try {
      if (logAnalyticsWorkspaceId != null) {
        armManagers.logAnalyticsManager().workspaces().deleteById(logAnalyticsWorkspaceId);
      }
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error(
            "Log analytics workspace doesn't exist or has been already deleted. Id={}",
            logAnalyticsWorkspaceId);
        return StepResult.getStepResultSuccess();
      }
      logger.error(
          "Failed attempt to delete log analytics workspace. Id={}", logAnalyticsWorkspaceId);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var logAnalyticsName =
        resourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_LOG_ANALYTICS_WORKSPACE_NAME_LENGTH);

    var logAnalyticsWorkspace =
        armManagers
            .logAnalyticsManager()
            .workspaces()
            .define(logAnalyticsName)
            .withRegion(resourceGroup.region())
            .withExistingResourceGroup(resourceGroup.id())
            .withRetentionInDays(
                context
                    .getInputParameters()
                    .get(
                        CromwellBaseResourcesFactory.ParametersNames.AUDIT_LOG_RETENTION_DAYS
                            .name(),
                        Integer.class))
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();

    context.getWorkingMap().put(LOG_ANALYTICS_WORKSPACE_ID, logAnalyticsWorkspace.id());
    context
        .getWorkingMap()
        .put(
            LOG_ANALYTICS_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(logAnalyticsWorkspace.id())
                .resourceType(logAnalyticsWorkspace.type())
                .tags(logAnalyticsWorkspace.tags())
                .region(logAnalyticsWorkspace.regionName())
                .resourceName(logAnalyticsWorkspace.name())
                .build());
    logger.info(
        RESOURCE_CREATED, getResourceType(), logAnalyticsWorkspace.id(), resourceGroup.name());
  }

  @Override
  protected String getResourceType() {
    return "LogAnalyticsWorkspace";
  }
}
