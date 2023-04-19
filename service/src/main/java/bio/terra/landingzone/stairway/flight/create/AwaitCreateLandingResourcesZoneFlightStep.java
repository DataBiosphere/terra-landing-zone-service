package bio.terra.landingzone.stairway.flight.create;

import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;

public class AwaitCreateLandingResourcesZoneFlightStep implements Step {

  private final String jobIdKey;

  public AwaitCreateLandingResourcesZoneFlightStep(String jobIdKey) {
    this.jobIdKey = jobIdKey;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    return null;
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return null;
  }
}
