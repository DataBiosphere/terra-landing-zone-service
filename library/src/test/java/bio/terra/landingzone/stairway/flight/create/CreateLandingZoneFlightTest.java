package bio.terra.landingzone.stairway.flight.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.resource.step.BaseResourceCreateStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateLandingZoneFlightTest {
  private static final UUID BILLING_PROFILE_ID = UUID.randomUUID();
  private static final UUID LANDING_ZONE_ID = UUID.randomUUID();

  private CreateLandingZoneFlight createLandingZoneFlight;

  @Mock private LandingZoneFlightBeanBag mockApplicationContext;

  // validating here only failed instantiation, otherwise it is required to
  // inject real value of tenant, subscription, mrg to initialize arm managers

  @Disabled(
      "adding GetManagedResourceGroupInfo requires us to initialize armManagers even when attaching to existing resources")
  @Test
  void testInitializationWhenAttaching() {
    final boolean isAttaching = true;
    LandingZoneRequest defaultLandingZoneRequest = createDefaultLandingZoneRequest(isAttaching);

    FlightMap inputParameters =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, defaultLandingZoneRequest));

    createLandingZoneFlight = new CreateLandingZoneFlight(inputParameters, mockApplicationContext);

    var steps = createLandingZoneFlight.getSteps();
    assertThat(steps.size(), equalTo(2));
    validateSteps(steps, isAttaching);
  }

  void validateSteps(List<Step> steps, boolean isAttaching) {
    assertThat(steps.stream().filter(s -> s instanceof CreateSamResourceStep).count(), equalTo(1L));
    assertThat(
        steps.stream().filter(s -> s instanceof GetManagedResourceGroupInfo).count(), equalTo(1L));
    if (!isAttaching) {
      // don't want to test for the exact number because it's perfectly valid that it can change
      // over time,
      // and this isn't a test for the step factory
      // instead, just picking a number that is solidly in the range of the expected number for
      // a sanity check
      assertThat(
          steps.stream()
              .filter(s -> BaseResourceCreateStep.class.isAssignableFrom(s.getClass()))
              .count(),
          greaterThan(10L));
    }
    assertThat(
        steps.stream().filter(s -> s instanceof CreateAzureLandingZoneDbRecordStep).count(),
        equalTo(1L));
  }

  @Test
  void testInstantiationWhenLandingZoneRequestParamDoesntExistThrowsException() {
    FlightMap inputParamsMap =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.randomUUID()));

    assertThrows(
        LandingZoneCreateException.class,
        () -> new CreateLandingZoneFlight(inputParamsMap, mockApplicationContext));
  }

  @Test
  void testInstantiationWhenLandingZoneIdParamDoesntExistThrowsException() {
    FlightMap inputParamsMap =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
                new LandingZoneRequest(
                    StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE
                        .getValue(),
                    "v1",
                    Map.of(),
                    BILLING_PROFILE_ID,
                    Optional.empty()) // no landing zone id, so the flight will look for one in the
                // input parameters
                ));
    assertThrows(
        LandingZoneCreateException.class,
        () -> new CreateLandingZoneFlight(inputParamsMap, mockApplicationContext));
  }

  LandingZoneRequest createDefaultLandingZoneRequest(boolean isAttaching) {
    return new LandingZoneRequest(
        StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue(),
        "v1",
        isAttaching ? Map.of(LandingZoneFlightMapKeys.ATTACH, "true") : Map.of(),
        BILLING_PROFILE_ID,
        Optional.of(LANDING_ZONE_ID));
  }
}
