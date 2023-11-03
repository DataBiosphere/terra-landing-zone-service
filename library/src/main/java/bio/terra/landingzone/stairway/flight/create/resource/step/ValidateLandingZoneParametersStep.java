package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.InputParameterValidator;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import java.util.List;
import java.util.Objects;

public class ValidateLandingZoneParametersStep implements Step {

  private final ParametersResolver parametersResolver;
  private final List<InputParameterValidator> inputParameterValidators;

  public ValidateLandingZoneParametersStep(
      List<InputParameterValidator> validators, ParametersResolver parametersResolver) {
    this.inputParameterValidators =
        Objects.requireNonNull(validators, "validators must not be null");
    this.parametersResolver =
        Objects.requireNonNull(parametersResolver, "parametersResolver must not be null");
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    try {
      inputParameterValidators.forEach(v -> v.validate(parametersResolver));
    } catch (InvalidInputParameterException e) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return StepResult.getStepResultSuccess();
  }
}
