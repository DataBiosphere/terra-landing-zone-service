package bio.terra.landingzone.stairway.flight.create;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.service.bpm.BillingProfileManagerService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;

/** Resolves the billing profile in BPM for use downstream in CreateLandingZoneFlight. */
public class GetBillingProfileStep implements Step {
  private final BillingProfileManagerService bpmService;

  public GetBillingProfileStep(BillingProfileManagerService bpmService) {
    this.bpmService = bpmService;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightMap inputMap = context.getInputParameters();
    FlightUtils.validateRequiredEntries(
        inputMap,
        LandingZoneFlightMapKeys.BEARER_TOKEN,
        LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS);

    var bearerToken = inputMap.get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
    var requestedLandingZone =
        inputMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class);

    // Call BPM to resolve the billing profile, and put it in the working map.
    try {
      var profile =
          bpmService.getBillingProfile(bearerToken, requestedLandingZone.billingProfileId());
      context.getWorkingMap().put(LandingZoneFlightMapKeys.BILLING_PROFILE, profile);
      return StepResult.getStepResultSuccess();
    } catch (Exception e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
