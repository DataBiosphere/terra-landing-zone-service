package bio.terra.landingzone.stairway.flight.delete;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.UUID;

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

    addDeleteSteps(flightBeanBag, inputParameters);
  }

  private void addDeleteSteps(LandingZoneFlightBeanBag flightBeanBag, FlightMap inputParameters) {
    FlightUtils.validateRequiredEntries(inputParameters, LandingZoneFlightMapKeys.LANDING_ZONE_ID);
    var landingZoneId = inputParameters.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var landingZoneDao = flightBeanBag.getLandingZoneDao();

    // Look up the landing zone record from the database
    LandingZoneRecord landingZoneRecord = landingZoneDao.getLandingZoneRecord(landingZoneId);

    if (!isAttaching(landingZoneRecord)) {
      addStep(
          new DeleteLandingZoneResourcesStep(
              flightBeanBag.getAzureLandingZoneManagerProvider(),
              flightBeanBag.getLandingZoneDao()),
          RetryRules.shortExponential());
    }

    addStep(
        new DeleteAzureLandingZoneDbRecordStep(flightBeanBag.getLandingZoneDao()),
        RetryRules.shortDatabase());

    if (!isAttaching(landingZoneRecord)) {
      addStep(
          new DeleteSamResourceStep(flightBeanBag.getSamService()), RetryRules.shortExponential());
    }
  }

  private boolean isAttaching(LandingZoneRecord record) {
    return Boolean.parseBoolean(
        record.properties().getOrDefault(LandingZoneFlightMapKeys.ATTACH, "false"));
  }
}
