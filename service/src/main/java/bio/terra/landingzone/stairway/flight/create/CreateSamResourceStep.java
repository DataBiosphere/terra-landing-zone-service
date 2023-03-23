package bio.terra.landingzone.stairway.flight.create;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
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

public class CreateSamResourceStep implements Step {
  private static final Logger logger = LoggerFactory.getLogger(CreateSamResourceStep.class);

  private final LandingZoneSamService samService;
  private final boolean isAttaching;

  public CreateSamResourceStep(LandingZoneSamService samService, boolean isAttaching) {
    this.samService = samService;
    this.isAttaching = isAttaching;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap,
        LandingZoneFlightMapKeys.BEARER_TOKEN,
        LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
        LandingZoneFlightMapKeys.LANDING_ZONE_ID);

    var bearerToken = inputMap.get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
    var requestedExternalLandingZoneResource =
        inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    samService.createLandingZone(
        bearerToken,
        requestedExternalLandingZoneResource.billingProfileId(),
        landingZoneId,
        isAttaching);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    if (this.isAttaching) {
      // do not delete the Sam resource if we are going to be reusing the LZ via attachment
      return StepResult.getStepResultSuccess();
    }

    final FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap, LandingZoneFlightMapKeys.BEARER_TOKEN, LandingZoneFlightMapKeys.LANDING_ZONE_ID);

    var bearerToken = inputMap.get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
    var landingZoneId = inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    samService.deleteLandingZone(bearerToken, landingZoneId);
    return StepResult.getStepResultSuccess();
  }
}
