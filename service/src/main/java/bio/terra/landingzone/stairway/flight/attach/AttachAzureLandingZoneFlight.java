package bio.terra.landingzone.stairway.flight.attach;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.stairway.flight.create.CreateAzureLandingZoneDbRecordStep;
import bio.terra.landingzone.stairway.flight.create.CreateSamResourceStep;
import bio.terra.landingzone.stairway.flight.create.GetBillingProfileStep;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;

public class AttachAzureLandingZoneFlight extends Flight {
  /**
   * All subclasses must provide a constructor with this signature.
   *
   * @param inputParameters FlightMap of the inputs for the flight
   * @param applicationContext Anonymous context meaningful to the application using Stairway
   */
  public AttachAzureLandingZoneFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);
    final LandingZoneFlightBeanBag flightBeanBag =
        LandingZoneFlightBeanBag.getFromObject(applicationContext);
    addCreateSteps(flightBeanBag);
  }

  private void addCreateSteps(LandingZoneFlightBeanBag flightBeanBag) {
    addStep(
        new CreateSamResourceStep(flightBeanBag.getSamService()), RetryRules.shortExponential());
    addStep(
        new GetBillingProfileStep(flightBeanBag.getBpmService()), RetryRules.shortExponential());
    addStep(
        new CreateAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());
  }
}
