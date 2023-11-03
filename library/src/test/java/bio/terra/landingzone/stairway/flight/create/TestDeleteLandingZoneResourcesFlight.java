package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.stairway.flight.delete.DeleteLandingZoneResourcesStep;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;

/**
 * This is a test flight only. It is needed to test deletion of LZ resources in an isolation without
 * Sam and Db interaction. "Real" deletion flight interacts with Sam to delete specific resource and
 * cleans up a database record associated with a landing zone.
 */
public class TestDeleteLandingZoneResourcesFlight extends Flight {

  /**
   * All subclasses must provide a constructor with this signature.
   *
   * @param inputParameters FlightMap of the inputs for the flight
   * @param applicationContext Anonymous context meaningful to the application using Stairway
   */
  public TestDeleteLandingZoneResourcesFlight(
      FlightMap inputParameters, Object applicationContext) {
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
  }
}
