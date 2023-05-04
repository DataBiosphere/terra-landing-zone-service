package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.mockito.Mock;

public class BaseStepTest {
  protected static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  protected static final String VNET_NAME = "vNet";
  protected static final String VNET_ID = "networkId";

  @Mock protected ArmManagers mockArmManagers;
  @Mock protected ParametersResolver mockParametersResolver;
  @Mock protected ResourceNameGenerator mockResourceNameGenerator;
  @Mock protected FlightContext mockFlightContext;

  protected void setupFlightContext(
      FlightContext flightContext,
      Map<String, Object> inputParameters,
      Map<String, Object> workingMap) {
    if (inputParameters != null) {
      FlightMap inputParamsMap = FlightTestUtils.prepareFlightInputParameters(inputParameters);
      when(flightContext.getInputParameters()).thenReturn(inputParamsMap);
    }
    if (workingMap != null) {
      FlightMap workingParamMap = FlightTestUtils.prepareFlightWorkingParameters(workingMap);
      when(flightContext.getWorkingMap()).thenReturn(workingParamMap);
    }
  }

  protected static LandingZoneResource buildLandingZoneResource() {
    return new LandingZoneResource(
        "resourceId",
        "resourceType",
        Map.of(),
        "region",
        Optional.of("resourceName"),
        Optional.empty());
  }
}
