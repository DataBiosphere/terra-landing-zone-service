package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;

import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneStepsDefinitionProviderFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;

/** Flight for creation of a Landing Zone */
public class CreateLandingZoneFlight extends Flight {

  @Override
  public void addStep(Step step, RetryRule retryRule) {
    super.addStep(step, retryRule);
  }

  /**
   * All subclasses must provide a constructor with this signature.
   *
   * @param inputParameters FlightMap of the inputs for the flight
   * @param applicationContext Anonymous context meaningful to the application using Stairway
   */
  public CreateLandingZoneFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);
    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);

    addCreateSteps(flightBeanBag, inputParameters);
  }

  private void addCreateSteps(LandingZoneFlightBeanBag flightBeanBag, FlightMap inputParameters) {
    var requestedLandingZone =
        inputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    if (requestedLandingZone == null) {
      throw new LandingZoneCreateException("Unable to find requested landing zone in input map");
    }

    addStep(
        new CreateSamResourceStep(flightBeanBag.getSamService()), RetryRules.shortExponential());

    addStep(
        new GetBillingProfileStep(flightBeanBag.getBpmService()), RetryRules.shortExponential());

    if (!requestedLandingZone.isAttaching()) {
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
          .get(resourceNameProvider, landingZoneProtectedDataConfiguration)
          .forEach(pair -> addStep(pair.getLeft(), pair.getRight()));

      // last step to aggregate results
      addStep(new AggregateLandingZoneResourcesStep(), RetryRules.shortExponential());
    }

    addStep(
        new CreateAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());
  }
}
