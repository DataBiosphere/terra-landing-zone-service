package bio.terra.landingzone.stairway.flight.create;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.FlightNotFoundException;
import bio.terra.stairway.exception.RetryException;

public class CreateLandingZoneResourcesFlightStep implements Step {

  private final LandingZoneJobService azureLandingZoneJobService;
  private final LandingZoneRequest landingZoneRequest;
  private final String jobIdKey;

  public CreateLandingZoneResourcesFlightStep(
      LandingZoneJobService azureLandingZoneJobService,
      LandingZoneRequest landingZoneRequest,
      String jobIdKey) {
    this.azureLandingZoneJobService = azureLandingZoneJobService;
    this.landingZoneRequest = landingZoneRequest;
    this.jobIdKey = jobIdKey;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    // initialize CreateLandingZoneResourcesFlight
    var jobDescription =
        "Inner flight to create landing zone resources. definition='%s', version='%s'";
    var jobId = String.format("lzResources_%s", landingZoneRequest.landingZoneId());
    // TODO: check if we already have flight

    if (isFlightAlreadyExists(jobId)) {
      return StepResult.getStepResultSuccess();
    }

    // this parameter should be read in AwayCreatLandingZoneFlightStep
    context.getWorkingMap().put(jobIdKey, jobId);

    // use token from parent flight
    var bearerToken =
        context.getWorkingMap().get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
    var resultPath = context.getWorkingMap().get(JobMapKeys.RESULT_PATH.toString(), String.class);

    // create sub-flight, which is supposed to create all required Azure resources
    azureLandingZoneJobService
        .newJob()
        .jobId(jobId)
        .description(
            String.format(
                jobDescription, landingZoneRequest.definition(), landingZoneRequest.version()))
        .flightClass(CreateLandingZoneResourcesFlight.class)
        .landingZoneRequest(landingZoneRequest)
        .operationType(OperationType.CREATE)
        .bearerToken(bearerToken) // <- this is required?
        .addParameter(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, landingZoneRequest)
        .addParameter(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneRequest.landingZoneId())
        .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath)
        .submit(); // TODO: <- check resultPath

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // nothing to undo
    return StepResult.getStepResultSuccess();
  }

  private boolean isFlightAlreadyExists(String jobId) {
    boolean flightAlreadyExists = true;
    try {

    } catch (FlightNotFoundException e) {
      flightAlreadyExists = true;
    }
    return flightAlreadyExists;
  }
}
