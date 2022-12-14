package bio.terra.landingzone.stairway.flight.delete;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
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

public class DeleteSamResourceStep implements Step {

  private final LandingZoneSamService landingZoneSamService;

  private static final Logger logger = LoggerFactory.getLogger(DeleteSamResourceStep.class);

  public DeleteSamResourceStep(LandingZoneSamService landingZoneSamService) {
    this.landingZoneSamService = landingZoneSamService;
  }

  @Override
  public StepResult doStep(FlightContext context) throws RetryException, InterruptedException {

    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap, LandingZoneFlightMapKeys.BEARER_TOKEN, LandingZoneFlightMapKeys.LANDING_ZONE_ID);

    var bearerToken = inputMap.get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    landingZoneSamService.deleteLandingZone(bearerToken, landingZoneId);
    logger.info("Deleted Sam resource for landing zone");
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // No undo for delete. There is no way to put it back.
    logger.error("Cannot undo delete of Sam resource.");
    // Surface whatever error caused Stairway to begin undoing.
    return context.getResult();
  }
}
