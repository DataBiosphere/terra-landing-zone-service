package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneStepsDefinitionProviderFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.resource.step.AggregateLandingZoneResourcesStep;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.*;
import bio.terra.stairway.exception.RetryException;
import java.util.UUID;

/**
 * This is a test flight only. It is intended to test the creation of LZ resources in an isolation
 * without Sam and Db interactions.
 */
public class TestCreateLandingZoneResourcesFlight extends Flight {

  // A Flight normally doesn't have the ability to add to the working map before it is run.
  // This is a step to add the billing profile to the working map, without calling bpm with the
  // GetBillingProfileStep
  public class TestBillingProfileStep implements Step {
    private final ProfileModel profile;

    public TestBillingProfileStep(ProfileModel profile) {
      super();
      this.profile = profile;
    }

    @Override
    public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
      context.getWorkingMap().put(LandingZoneFlightMapKeys.BILLING_PROFILE, profile);
      return StepResult.getStepResultSuccess();
    }

    @Override
    public StepResult undoStep(FlightContext context) throws InterruptedException {
      return StepResult.getStepResultSuccess();
    }
  }

  public TestCreateLandingZoneResourcesFlight(
      FlightMap inputParameters, Object applicationContext, ProfileModel profile) {
    super(inputParameters, applicationContext);
    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);
    addStep(new TestBillingProfileStep(profile), RetryRules.shortExponential());
    addCreateSteps(flightBeanBag, inputParameters);
  }

  private void addCreateSteps(LandingZoneFlightBeanBag flightBeanBag, FlightMap inputParameters) {
    var requestedLandingZone =
        inputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    if (requestedLandingZone == null) {
      throw new LandingZoneCreateException("Unable to find requested landing zone in input map");
    }

    var stepsDefinitionProvider =
        LandingZoneStepsDefinitionProviderFactory.create(
            StepsDefinitionFactoryType.fromString(requestedLandingZone.definition()));
    var landingZoneId = getLandingZoneId(inputParameters, requestedLandingZone);

    var resourceNameProvider = new ResourceNameProvider(landingZoneId);

    var parametersResolver =
        new ParametersResolver(
            requestedLandingZone.parameters(), LandingZoneDefaultParameters.get());
    var landingZoneProtectedDataConfiguration =
        flightBeanBag.getLandingZoneProtectedDataConfiguration();

    addStep(new InitializeArmManagersStep(), RetryRules.shortExponential());
    stepsDefinitionProvider
        .get(parametersResolver, resourceNameProvider, landingZoneProtectedDataConfiguration)
        .forEach(pair -> addStep(pair.getLeft(), pair.getRight()));

    // last step to aggregate results
    addStep(new AggregateLandingZoneResourcesStep(), RetryRules.shortExponential());
  }

  private UUID getLandingZoneId(FlightMap inputParameters, LandingZoneRequest landingZoneRequest) {
    // landing zone identifier can come in request's body or we generate it and keep it separately
    if (landingZoneRequest.landingZoneId().isPresent()) {
      return landingZoneRequest.landingZoneId().get();
    } else {
      var landingZoneId = inputParameters.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
      if (landingZoneId == null) {
        throw new LandingZoneCreateException("Unable to find landing zone identifier in input map");
      }
      return landingZoneId;
    }
  }
}
