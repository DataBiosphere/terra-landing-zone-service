package bio.terra.landingzone.stairway.flight.create;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
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

    final BearerToken bearerToken =
            inputParameters.get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);

    addCreateSteps(flightBeanBag, bearerToken);
  }

  private void addCreateSteps(LandingZoneFlightBeanBag flightBeanBag, BearerToken bearerToken) {

    addStep(
            new GetBillingProfileStep(flightBeanBag.getBpmService()), RetryRules.shortExponential());

    addStep(
        new CreateAzureLandingZoneStep(flightBeanBag.getAzureLandingZoneManagerProvider()),
        RetryRules.cloud());

    addStep(
        new CreateAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());

    addStep(
            new CreateSamResourceStep(flightBeanBag.getSamService()),
            RetryRules.shortExponential());
  }
}
