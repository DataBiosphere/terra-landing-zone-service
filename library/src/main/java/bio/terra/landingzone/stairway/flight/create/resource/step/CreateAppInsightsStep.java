package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAppInsightsStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateAppInsightsStep.class);
  private static final String INVALID_LAW_MSG =
      "Could not retrieve the Log Analytics workspace from ARM";
  public static final String APP_INSIGHT_ID = "APP_INSIGHT_ID";

  public CreateAppInsightsStep(ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);

    var appInsightsName = resourceNameProvider.getName(getResourceType());
    var appInsight =
        armManagers
            .applicationInsightsManager()
            .components()
            .define(appInsightsName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withKind("java")
            .withApplicationType(ApplicationType.OTHER)
            .withWorkspaceResourceId(logAnalyticsWorkspaceId)
            .withTags(
                Map.of(
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
                    LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                    ResourcePurpose.SHARED_RESOURCE.toString()))
            .create();
    context.getWorkingMap().put(APP_INSIGHT_ID, appInsight.id());
    logger.info(RESOURCE_CREATED, getResourceType(), appInsight.id(), getMRGName(context));
  }

  @Override
  protected Optional<StepResult> maybeHandleManagementException(ManagementException e) {
    if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "BadRequest")
        && e.getValue().getMessage().contains(INVALID_LAW_MSG)) {
      // the log analytics workspace may not be "visible" yet, so we retry
      logger.warn("Bad request while creating app insights, retrying.", e);
      return Optional.of(new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY));
    }
    return Optional.empty();
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.applicationInsightsManager().components().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "AppInsights";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(APP_INSIGHT_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_APP_INSIGHTS_COMPONENT_NAME_LENGTH));
  }
}
