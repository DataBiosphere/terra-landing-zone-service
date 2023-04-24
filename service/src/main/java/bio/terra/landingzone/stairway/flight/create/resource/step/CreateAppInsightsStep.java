package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAppInsightsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateAppInsightsStep.class);
  public static final String APP_INSIGHT_ID = "APP_INSIGHT_ID";

  public CreateAppInsightsStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
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

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    var appInsightsName =
        resourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_APP_INSIGHTS_COMPONENT_NAME_LENGTH);
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
    logger.info(RESOURCE_CREATED, getResourceType(), appInsight.id(), resourceGroup.name());
  }

  @Override
  protected String getResourceType() {
    return "AppInsights";
  }
}
