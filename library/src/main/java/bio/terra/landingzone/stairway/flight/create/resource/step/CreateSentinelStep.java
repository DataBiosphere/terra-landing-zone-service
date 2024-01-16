package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.FlightContext;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSentinelStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateSentinelStep.class);
  public static final String SENTINEL_ID = "SENTINEL_ID";
  public static final String SENTINEL_RESOURCE_KEY = "SENTINEL";

  public CreateSentinelStep(ResourceNameProvider resourceNameProvider) {
    super(resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var logAnalyticsWorkspace =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            LandingZoneResource.class);

    var logAnalyticsWorkspaceResourceName = logAnalyticsWorkspace.resourceName();
    if (logAnalyticsWorkspaceResourceName.isEmpty()) {
      throw new MissingRequiredFieldsException("LogAnalyticsWorkspace resource name is not set.");
    }

    var state =
        armManagers
            .securityInsightsManager()
            .sentinelOnboardingStates()
            .define("default")
            .withExistingWorkspace(getMRGName(context), logAnalyticsWorkspaceResourceName.get())
            .create();

    context.getWorkingMap().put(SENTINEL_ID, state.id());
    context
        .getWorkingMap()
        .put(
            SENTINEL_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(state.id())
                .resourceType(state.type())
                .resourceName(state.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), state.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId, ArmManagers armManagers) {
    armManagers.securityInsightsManager().sentinelOnboardingStates().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "Sentinel";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(SENTINEL_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    // we don't generate name for sentinel. Azure accepts only 'default' as a name
    return List.of();
  }
}
