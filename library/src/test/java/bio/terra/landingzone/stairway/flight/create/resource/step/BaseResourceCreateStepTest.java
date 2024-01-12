package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class BaseResourceCreateStepTest extends BaseStepTest {
  @Test
  void testDoStepThrowsInterruptedException() {
    var step =
        new BaseResourceCreateStep(mockArmManagers, mockResourceNameProvider) {
          @Override
          public List<ResourceNameRequirements> getResourceNameRequirements() {
            return null;
          }

          @Override
          protected void createResource(FlightContext context, ArmManagers armManagers) {
            throw new RuntimeException("Interrupted", new InterruptedException());
          }

          @Override
          protected void deleteResource(String resourceId) {}

          @Override
          protected String getResourceType() {
            return "someResource";
          }

          @Override
          protected Optional<String> getResourceId(FlightContext context) {
            return Optional.empty();
          }
        };

    FlightMap inputParamsMap = new FlightMap();
    inputParamsMap.put(
        LandingZoneFlightMapKeys.BILLING_PROFILE, new ProfileModel().id(UUID.randomUUID()));
    inputParamsMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, LANDING_ZONE_ID);
    inputParamsMap.put(
        LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
        ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone());
    when(mockFlightContext.getInputParameters()).thenReturn(inputParamsMap);
    Assertions.assertThrows(InterruptedException.class, () -> step.doStep(mockFlightContext));
    assertThat(Thread.currentThread().isInterrupted(), equalTo(true));
  }
}
