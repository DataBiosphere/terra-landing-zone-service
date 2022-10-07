package bio.terra.landingzone.stairway.flight.delete;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;

public class DeleteLandingZoneFlight extends Flight {
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
  public DeleteLandingZoneFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);
    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);

    addDeleteSteps(flightBeanBag);
  }

  private void addDeleteSteps(LandingZoneFlightBeanBag flightBeanBag) {
    addStep(
        new DeleteLandingZoneResourcesStep(
            flightBeanBag.getAzureLandingZoneManagerProvider(), flightBeanBag.getLandingZoneDao()),
        RetryRules.shortExponential());

    addStep(
        new DeleteAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());
  }
}
