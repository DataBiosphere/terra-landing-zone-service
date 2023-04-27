package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwaitCreateLandingResourcesZoneFlightStep implements Step {
  private static final Logger logger =
      LoggerFactory.getLogger(AwaitCreateLandingResourcesZoneFlightStep.class);
  public static final int FLIGHT_POLL_SECONDS = 1;
  // successful flight takes 10 min to deploy all resources,
  // in case last step failed we need to delete all the resources
  // let's limit such scenario with 30 min.
  public static final int FLIGHT_POLL_CYCLES = 1800;

  private final String jobIdKey;

  public AwaitCreateLandingResourcesZoneFlightStep(String jobIdKey) {
    this.jobIdKey = jobIdKey;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var subFlightId = context.getWorkingMap().get(jobIdKey, String.class);
    FlightState subFlightState =
        context.getStairway().waitForFlight(subFlightId, FLIGHT_POLL_SECONDS, FLIGHT_POLL_CYCLES);
    if (subFlightState.getFlightStatus() != FlightStatus.SUCCESS) {
      return new StepResult(
          StepStatus.STEP_RESULT_FAILURE_FATAL,
          subFlightState
              .getException()
              .orElseGet(
                  () ->
                      new LandingZoneCreateException(
                          "Failed to create landing zone Azure resources.")));
    }

    if (subFlightState.getResultMap().isPresent()) {
      // we need to pass result from sub-flight
      var deployedLandingZone =
          subFlightState
              .getResultMap()
              .get()
              .get(JobMapKeys.RESPONSE.getKeyName(), DeployedLandingZone.class);
      context.getWorkingMap().put(JobMapKeys.RESPONSE.getKeyName(), deployedLandingZone);
    } else {
      return new StepResult(
          StepStatus.STEP_RESULT_FAILURE_FATAL,
          new LandingZoneCreateException(
              String.format(
                  "Expected landing zone result from sub-flight with id='%s' not found.",
                  subFlightId)));
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
