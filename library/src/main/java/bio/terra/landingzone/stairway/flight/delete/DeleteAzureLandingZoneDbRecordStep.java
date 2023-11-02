package bio.terra.landingzone.stairway.flight.delete;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteAzureLandingZoneDbRecordStep implements Step {
  private final LandingZoneDao landingZoneDao;
  private static final Logger logger =
      LoggerFactory.getLogger(DeleteAzureLandingZoneDbRecordStep.class);

  public DeleteAzureLandingZoneDbRecordStep(LandingZoneDao landingZoneDao) {
    this.landingZoneDao = landingZoneDao;
  }

  @Override
  public StepResult doStep(FlightContext context) throws RetryException {
    // Read input parameters
    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(inputMap, LandingZoneFlightMapKeys.LANDING_ZONE_ID);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    // Delete LZ record
    landingZoneDao.deleteLandingZone(landingZoneId);
    logger.info("Landing zone record deleted from the database. {}", landingZoneId);

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    // there is no good way to undo the deletion of resources
    // therefore undo the db record delete does not provide any benefit.
    return StepResult.getStepResultSuccess();
  }
}
