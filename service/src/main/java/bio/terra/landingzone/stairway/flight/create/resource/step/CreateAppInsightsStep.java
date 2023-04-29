package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
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
}
