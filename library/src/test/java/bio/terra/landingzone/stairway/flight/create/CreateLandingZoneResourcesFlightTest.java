package bio.terra.landingzone.stairway.flight.create;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.stairway.FlightMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateLandingZoneResourcesFlightTest {
  @Mock private FlightMap mockInputParameters;
  @Mock private LandingZoneFlightBeanBag mockApplicationContext;

  // validating here only failed instantiation, otherwise it is required to
  // inject real value of tenant, subscription, mrg to initialize arm managers

  @Test
  void testInstantiationWhenLandingZoneRequestParamDoesntExistThrowsException() {
    when(mockInputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class))
        .thenReturn(null);

    assertThrows(
        LandingZoneCreateException.class,
        () -> new CreateLandingZoneResourcesFlight(mockInputParameters, mockApplicationContext));
  }

  @Test
  void testInstantiationWhenLandingZoneIdParamDoesntExistThrowsException() {
    when(mockInputParameters.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class))
        .thenReturn(createDefaultLandingZoneRequest());

    assertThrows(
        LandingZoneCreateException.class,
        () -> new CreateLandingZoneResourcesFlight(mockInputParameters, mockApplicationContext));
  }

  private LandingZoneRequest createDefaultLandingZoneRequest() {
    return new LandingZoneRequest(
        StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue(),
        "v1",
        Map.of(),
        UUID.randomUUID(),
        Optional.empty());
  }
}
