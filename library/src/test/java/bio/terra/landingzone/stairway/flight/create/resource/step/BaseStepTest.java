package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import com.azure.resourcemanager.AzureResourceManager;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

class BaseStepTest {
  protected static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  protected static final String VNET_NAME = "vNet";
  protected static final String VNET_ID = "networkId";

  @Mock protected ArmManagers mockArmManagers;
  @Mock protected AzureResourceManager mockAzureResourceManager;
  protected ParametersResolver mockParametersResolver;
  @Mock protected ResourceNameProvider mockResourceNameProvider;
  @Mock protected FlightContext mockFlightContext;
  @Mock protected LandingZoneFlightBeanBag mockLandingZoneFlightBeanBag;
  @Captor protected ArgumentCaptor<Map<String, String>> tagsCaptor;

  protected void setupFlightContext(
      FlightContext flightContext,
      Map<String, Object> inputParameters,
      Map<String, Object> workingMap) {

    Mockito.lenient()
        .when(flightContext.getApplicationContext())
        .thenReturn(mockLandingZoneFlightBeanBag);

    if (inputParameters != null) {
      FlightMap inputParamsMap = FlightTestUtils.prepareFlightInputParameters(inputParameters);
      Mockito.lenient().when(flightContext.getInputParameters()).thenReturn(inputParamsMap);
    }
    if (workingMap != null) {
      FlightMap workingParamMap = FlightTestUtils.prepareFlightWorkingParameters(workingMap);
      Mockito.lenient().when(flightContext.getWorkingMap()).thenReturn(workingParamMap);
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

  /** Verifies that following tags assigned - WLZ-ID, WLZ-PURPOSE */
  protected void verifyBasicTags(Map<String, String> tags, UUID landingZoneId) {
    assertNotNull(tags);
    assertTrue(tags.containsKey(LandingZoneTagKeys.LANDING_ZONE_ID.toString()));
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()), equalTo(landingZoneId.toString()));
    assertTrue(tags.containsKey(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()));
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()),
        equalTo(ResourcePurpose.SHARED_RESOURCE.toString()));
  }

  protected static Stream<Arguments> inputParameterProvider() {
    return Stream.of(
        Arguments.of(Map.of(LandingZoneFlightMapKeys.LANDING_ZONE_ID, LANDING_ZONE_ID)),
        Arguments.of(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()))));
  }
}
