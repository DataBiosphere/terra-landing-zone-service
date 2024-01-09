package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateLogAnalyticsWorkspaceStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateLogAnalyticsWorkspaceStep.class);

  public static final String LOG_ANALYTICS_WORKSPACE_ID = "LOG_ANALYTICS_WORKSPACE_ID";
  public static final String LOG_ANALYTICS_RESOURCE_KEY = "LOG_ANALYTICS";

  public CreateLogAnalyticsWorkspaceStep(ResourceNameProvider resourceNameProvider) {
    super(resourceNameProvider);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    super.undoStep(context);

    if (getResourceId(context).isEmpty()) {
      return StepResult.getStepResultSuccess();
    }
    var armManagers =
        context.getWorkingMap().get(LandingZoneFlightMapKeys.ARM_MANAGERS_KEY, ArmManagers.class);

    try {
      // Deploying AKS with monitoring connected to a log analytics workspace also deploys a
      // container insights solution named `ContainerInsights(WORKSPACE_ID)` which is untagged
      // this code lists them all and later code figures out which to delete
      var solutions =
          armManagers
              .azureResourceManager()
              .genericResources()
              .listByResourceGroup(getMRGName(context))
              .stream()
              .filter(
                  r ->
                      AzureResourceTypeUtils.AZURE_SOLUTIONS_TYPE.equalsIgnoreCase(
                          "%s/%s".formatted(r.resourceProviderNamespace(), r.resourceType())))
              .toList();
      solutions.forEach(
          s -> {
            try {
              armManagers.azureResourceManager().genericResources().deleteById(s.id());
              logger.info(
                  "{} resource with id={} deleted.",
                  AzureResourceTypeUtils.AZURE_SOLUTIONS_TYPE,
                  s.id());
            } catch (ManagementException e) {
              logger.error(
                  "Failed attempt to delete ContainerInsight (might be already deleted or doesn't exist). Id={}",
                  s.id());
              throw e;
            }
          });
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      logger.error(
          "Failed attempt to delete ContainerInsight workspace. LogAnalyticsWorkspaceId={}",
          getResourceId(context));
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var logAnalyticsName = resourceNameProvider.getName(getResourceType());

    var logAnalyticsWorkspace =
        armManagers
            .logAnalyticsManager()
            .workspaces()
            .define(logAnalyticsName)
            .withRegion(getMRGRegionName(context))
            .withExistingResourceGroup(getMRGName(context))
            .withRetentionInDays(
                context
                    .getInputParameters()
                    .get(
                        LandingZoneDefaultParameters.ParametersNames.AUDIT_LOG_RETENTION_DAYS
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
        RESOURCE_CREATED, getResourceType(), logAnalyticsWorkspace.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId, FlightContext context) {
    var armManagers =
        context.getWorkingMap().get(LandingZoneFlightMapKeys.ARM_MANAGERS_KEY, ArmManagers.class);
    armManagers.logAnalyticsManager().workspaces().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "LogAnalyticsWorkspace";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(
        context.getWorkingMap().get(LOG_ANALYTICS_WORKSPACE_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_LOG_ANALYTICS_WORKSPACE_NAME_LENGTH));
  }
}
