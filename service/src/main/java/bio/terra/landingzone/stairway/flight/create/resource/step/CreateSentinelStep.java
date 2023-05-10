package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.FlightContext;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSentinelStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateSentinelStep.class);
  public static final String SENTINEL_ID = "SENTINEL_ID";
  public static final String SENTINEL_RESOURCE_KEY = "SENTINEL";

  public CreateSentinelStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var workspace =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            LandingZoneResource.class);

    if (workspace.resourceName().isEmpty()) {
      throw new MissingRequiredFieldsException("LogAnalyticsWorkspace resource name is not set.");
    }

    var state =
        armManagers
            .securityInsightsManager()
            .sentinelOnboardingStates()
            .define("default")
            .withExistingWorkspace(getMRGName(context), workspace.resourceName().get())
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
  protected void deleteResource(String resourceId) {
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
}
