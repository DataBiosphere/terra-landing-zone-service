package bio.terra.landingzone.library.landingzones.definition.factories.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReferencedVNetValidatorTest {

  private ReferencedVNetValidator validator;

  @Mock ParametersResolver mockParametersResolver;

  @BeforeEach
  void setUp() {
    validator = new ReferencedVNetValidator();
  }

  @Test
  void validate_AllParametersProvided_PassesValidation() {
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.BATCH_SUBNET.name()))
        .thenReturn("batch-subnet");
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.COMPUTE_SUBNET.name()))
        .thenReturn("compute-subnet");
    assertDoesNotThrow(() -> validator.validate(mockParametersResolver));
  }

  @Test
  void validate_BatchSubnetMissing_ThrowsException() {
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.BATCH_SUBNET.name()))
        .thenReturn(null);
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.COMPUTE_SUBNET.name()))
        .thenReturn("compute-subnet");
    assertThrows(
        InvalidInputParameterException.class, () -> validator.validate(mockParametersResolver));
  }

  @Test
  void validate_ComputeSubnetMissing_ThrowsException() {
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.BATCH_SUBNET.name()))
        .thenReturn("batch-subnet");
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.COMPUTE_SUBNET.name()))
        .thenReturn(null);
    assertThrows(
        InvalidInputParameterException.class, () -> validator.validate(mockParametersResolver));
  }

  @Test
  void validate_AllParametersMissing_ThrowsException() {
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.BATCH_SUBNET.name()))
        .thenReturn(null);
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.COMPUTE_SUBNET.name()))
        .thenReturn(null);
    assertThrows(
        InvalidInputParameterException.class, () -> validator.validate(mockParametersResolver));
  }
}
