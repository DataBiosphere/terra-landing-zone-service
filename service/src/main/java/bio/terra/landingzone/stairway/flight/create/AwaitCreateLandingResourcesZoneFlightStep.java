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

public class AwaitCreateLandingResourcesZoneFlightStep implements Step {
  public static final int FLIGHT_POLL_SECONDS = 1;
  public static final int FLIGHT_POLL_CYCLES = 1200;

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

    // we need to pass result from sub-flight
    var deployedLandingZone =
        subFlightState
            .getResultMap()
            .get()
            .get(JobMapKeys.RESPONSE.getKeyName(), DeployedLandingZone.class);
    context.getWorkingMap().put(JobMapKeys.RESPONSE.getKeyName(), deployedLandingZone);

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return null;
  }
}
