package bio.terra.landingzone.stairway.flight.create.resource.step;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreatePrivateEndpointStepTest extends BaseStepTest {
  private CreatePrivateEndpointStep createPrivateEndpointStep;

  @BeforeEach
  void setUp() {
    createPrivateEndpointStep =
        new CreatePrivateEndpointStep(
            mockArmManagers, mockParametersResolver, mockResourceNameGenerator);
  }
}
