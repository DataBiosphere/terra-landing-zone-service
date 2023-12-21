package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.BlobCorsParametersValidator;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.InputParameterValidator;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ValidateLandingZoneParametersStepTest {
  private ValidateLandingZoneParametersStep validateLandingZoneParametersStep;

  @Mock private FlightContext mockFlightContext;
  @Mock private ParametersResolver mockParametersResolver;

  @Test
  void testSuccessfulValidation() throws InterruptedException {
    var validator = mock(InputParameterValidator.class);
    doNothing().when(validator).validate(any());
    List<InputParameterValidator> validators = List.of(validator);
    validateLandingZoneParametersStep = new ValidateLandingZoneParametersStep(validators);

    StepResult stepResult = validateLandingZoneParametersStep.doStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(validator, times(1)).validate(any());
  }

  @Test
  void testFailedValidation() throws InterruptedException {
    var validator = mock(InputParameterValidator.class);
    doThrow(InvalidInputParameterException.class).when(validator).validate(any());
    List<InputParameterValidator> validators = List.of(validator);
    validateLandingZoneParametersStep = new ValidateLandingZoneParametersStep(validators);

    StepResult stepResult = validateLandingZoneParametersStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
    assertTrue(stepResult.getException().isPresent());
    assertThat(
        stepResult.getException().get().getClass(), equalTo(InvalidInputParameterException.class));
    verify(validator, times(1)).validate(any());
  }

  @ParameterizedTest
  @MethodSource("initializationParameters")
  void testInitialization(List<InputParameterValidator> validators) {
    assertThrows(
        NullPointerException.class, () -> new ValidateLandingZoneParametersStep(validators));
  }

  private static Stream<Arguments> initializationParameters() {
    return Stream.of(Arguments.of(List.of(new BlobCorsParametersValidator())), Arguments.of(null));
  }
}
