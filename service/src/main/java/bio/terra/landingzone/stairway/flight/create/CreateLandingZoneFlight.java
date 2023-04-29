package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
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
      if (Boolean.TRUE.equals(requestedLandingZone.useStairwayPath())) {
        addStep(
            new CreateLandingZoneResourcesFlightStep(
                flightBeanBag.getLandingZoneService(),
                requestedLandingZone,
                LandingZoneFlightMapKeys.CREATE_LANDING_ZONE_RESOURCES_INNER_FLIGHT_JOB_ID));
        addStep(
            new AwaitCreateLandingResourcesZoneFlightStep(
                LandingZoneFlightMapKeys.CREATE_LANDING_ZONE_RESOURCES_INNER_FLIGHT_JOB_ID));
      } else {
        addStep(
            new CreateAzureLandingZoneStep(flightBeanBag.getAzureLandingZoneManagerProvider()),
            RetryRules.cloud());
      }
    }

    addStep(
        new CreateAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());
  }
}
