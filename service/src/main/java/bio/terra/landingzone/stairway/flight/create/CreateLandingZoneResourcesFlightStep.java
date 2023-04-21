package bio.terra.landingzone.stairway.flight.create;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.FlightNotFoundException;
import bio.terra.stairway.exception.RetryException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CreateLandingZoneResourcesFlightStep implements Step {

  private final LandingZoneService landingZoneService;
  private final LandingZoneRequest landingZoneRequest;
  private final UUID landingZoneId;
  private final String jobIdKey;

  public CreateLandingZoneResourcesFlightStep(
      LandingZoneService landingZoneService,
      LandingZoneRequest landingZoneRequest,
      // TODO: check why this is not in landingZoneRequest
      UUID landingZoneId,
      String jobIdKey) {
    this.landingZoneService = landingZoneService;
    this.landingZoneRequest = landingZoneRequest;
    this.landingZoneId = landingZoneId;
    this.jobIdKey = jobIdKey;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    // TODO: jobId is limited to 36 characters
    var jobId = String.format("lzResources_%s", OffsetDateTime.now().toInstant().getEpochSecond());

    if (isFlightAlreadyExists(context, jobId)) {
      return StepResult.getStepResultSuccess();
    }

    // use token from parent flight
    var bearerToken =
        context.getInputParameters().get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
    var resultPath =
        context.getInputParameters().get(JobMapKeys.RESULT_PATH.toString(), String.class);
    var billingProfile =
        context.getWorkingMap().get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);

    // this parameter should be read in AwayCreatLandingZoneFlightStep
    context.getWorkingMap().put(jobIdKey, jobId);

    // create sub-flight, which is supposed to create all required Azure resources
    landingZoneService.startLandingZoneResourceCreationJob(
        jobId,
        landingZoneRequest,
        billingProfile,
        landingZoneId,
        bearerToken,
        resultPath + "subFlight");

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // nothing to undo
    return StepResult.getStepResultSuccess();
  }

  private boolean isFlightAlreadyExists(FlightContext context, String flightId)
      throws InterruptedException {
    boolean flightAlreadyExists = true;
    try {
      context.getStairway().getFlightState(flightId);
    } catch (FlightNotFoundException e) {
      flightAlreadyExists = false;
    }
    return flightAlreadyExists;
  }
}
