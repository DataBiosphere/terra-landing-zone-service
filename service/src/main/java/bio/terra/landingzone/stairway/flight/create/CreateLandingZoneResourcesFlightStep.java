package bio.terra.landingzone.stairway.flight.create;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.FlightNotFoundException;
import bio.terra.stairway.exception.RetryException;
import java.util.UUID;

public class CreateLandingZoneResourcesFlightStep implements Step {

  private final LandingZoneService landingZoneService;
  private final LandingZoneRequest landingZoneRequest;
  private final String jobIdKey;

  public CreateLandingZoneResourcesFlightStep(
      LandingZoneService landingZoneService,
      LandingZoneRequest landingZoneRequest,
      String jobIdKey) {
    this.landingZoneService = landingZoneService;
    this.landingZoneRequest = landingZoneRequest;
    this.jobIdKey = jobIdKey;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightUtils.validateRequiredEntries(
        context.getInputParameters(),
        LandingZoneFlightMapKeys.LANDING_ZONE_ID,
        LandingZoneFlightMapKeys.BEARER_TOKEN,
        JobMapKeys.RESULT_PATH.getKeyName());

    FlightUtils.validateRequiredEntries(
        context.getWorkingMap(), LandingZoneFlightMapKeys.BILLING_PROFILE);

    var landingZoneId =
        context.getInputParameters().get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var subflightId = getSubflightId(landingZoneId.toString());
    if (isFlightAlreadyExists(context, subflightId)) {
      return StepResult.getStepResultSuccess();
    }

    // use token from parent flight
    var bearerToken =
        context.getInputParameters().get(LandingZoneFlightMapKeys.BEARER_TOKEN, BearerToken.class);
    var resultPath =
        context.getInputParameters().get(JobMapKeys.RESULT_PATH.getKeyName(), String.class);
    var billingProfile =
        context.getWorkingMap().get(LandingZoneFlightMapKeys.BILLING_PROFILE, ProfileModel.class);

    // this parameter should be read in AwayCreatLandingZoneFlightStep
    context.getWorkingMap().put(jobIdKey, subflightId);

    // create sub-flight, which is supposed to create all required Azure resources
    landingZoneService.startLandingZoneResourceCreationJob(
        subflightId,
        landingZoneRequest,
        billingProfile,
        landingZoneId,
        bearerToken,
        resultPath + subflightId);

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

  private String getSubflightId(String landingZoneId) {
    // subFlightId is limited up to 36 characters
    // use first 8 characters and last 4 characters of landingzoneId
    int len = landingZoneId.length();
    return String.format(
        "resFlight_%s", landingZoneId.substring(0, 8) + "_" + landingZoneId.substring(len - 4));
  }
}
