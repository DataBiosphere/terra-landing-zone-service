package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAppInsightsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateAppInsightsStep.class);
  public static final String APP_INSIGHT_ID = "APP_INSIGHT_ID";

  public CreateAppInsightsStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);

    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    var appInsightsName =
        resourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_APP_INSIGHTS_COMPONENT_NAME_LENGTH);
    try {
      var appInsight =
          armManagers
              .applicationInsightsManager()
              .components()
              .define(appInsightsName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name())
              .withKind("java")
              .withApplicationType(ApplicationType.OTHER)
              .withWorkspaceResourceId(logAnalyticsWorkspaceId)
              .create();
      context.getWorkingMap().put(APP_INSIGHT_ID, appInsight.id());
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS, "Application insights", appInsightsName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "application insights", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    var appInsightId = context.getWorkingMap().get(APP_INSIGHT_ID, String.class);
    try {
      armManagers.applicationInsightsManager().components().deleteById(appInsightId);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }
}
