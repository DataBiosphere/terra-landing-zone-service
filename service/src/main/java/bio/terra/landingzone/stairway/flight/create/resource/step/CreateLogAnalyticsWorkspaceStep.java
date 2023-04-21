package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
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
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);
    // TODO: check if we can arrange all these dependencies in a different way
    // Most like we need the same setup for different steps. At least we need armManagers.
    var logAnalyticsName =
        resourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_LOG_ANALYTICS_WORKSPACE_NAME_LENGTH);
    try {
      //      var logAnalyticsWorkspace =
      //          armManagers
      //              .logAnalyticsManager()
      //              .workspaces()
      //              .define(logAnalyticsName)
      //              .withRegion(resourceGroup.region())
      //              .withExistingResourceGroup(resourceGroup.id())
      //              .withRetentionInDays(
      //                  context
      //                      .getInputParameters()
      //                      .get(
      //
      // CromwellBaseResourcesFactory.ParametersNames.AUDIT_LOG_RETENTION_DAYS
      //                              .name(),
      //                          Integer.class))
      //              .withTags(
      //                  Map.of(
      //                      LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
      //                      landingZoneId.toString(),
      //                      LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
      //                      ResourcePurpose.SHARED_RESOURCE.toString()))
      //              .create();

      context.getWorkingMap().put(LOG_ANALYTICS_WORKSPACE_ID, "logAnalyticsWorkspace.id()");

      String logAnalyticsId = "logAnalyticsId";
      context
          .getWorkingMap()
          .put(
              LOG_ANALYTICS_RESOURCE_KEY,
              LandingZoneResource.builder().resourceId(logAnalyticsId).build());
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS, "Log analytics", logAnalyticsName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "log analytics", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    LandingZoneResource logAnalyticsWorkspaceResource =
        context.getWorkingMap().get(LOG_ANALYTICS_RESOURCE_KEY, LandingZoneResource.class);
    try {
      armManagers
          .logAnalyticsManager()
          .workspaces()
          .deleteById(logAnalyticsWorkspaceResource.resourceId());
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }
}
