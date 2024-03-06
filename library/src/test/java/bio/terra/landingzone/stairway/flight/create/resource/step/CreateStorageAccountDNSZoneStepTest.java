package bio.terra.landingzone.stairway.flight.create.resource.step;

import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountDNSZoneStep.STORAGE_ACCOUNT_DNS_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.azure.resourcemanager.privatedns.models.PrivateDnsZones;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CreateStorageAccountDNSZoneStepTest extends BaseStepTest {
  private CreateStorageAccountDNSZoneStep testStep;
  @Mock private PrivateDnsZones mockPrivateDnsZones;
  @Mock private PrivateDnsZone.DefinitionStages.Blank mockDefine;
  @Mock private PrivateDnsZone.DefinitionStages.WithCreate mockWithCreate;
  @Mock private PrivateDnsZone mockPrivateDnsZone;

  @BeforeEach
  void setup() {
    testStep = new CreateStorageAccountDNSZoneStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String resourceName = UUID.randomUUID().toString();
    final String dnsId = UUID.randomUUID().toString();

    when(mockPrivateDnsZone.id()).thenReturn(dnsId);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg));
    setupArmManagersForDoStep(resourceName, mrg);

    // Run step
    var stepResult = testStep.doStep(mockFlightContext);

    // Verify step succeeded and populated the working map
    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(CreateStorageAccountDNSZoneStep.STORAGE_ACCOUNT_DNS_ID, String.class),
        equalTo(dnsId));

    // Verify captors
    var tags = tagsCaptor.getValue();
    assertNotNull(tags);
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()),
        equalTo(LANDING_ZONE_ID.toString()));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    var resourceId = "resourceId";
    workingMap.put(CreateStorageAccountDNSZoneStep.STORAGE_ACCOUNT_DNS_ID, resourceId);
    workingMap.put(
        GetManagedResourceGroupInfo.TARGET_MRG_KEY, ResourceStepFixture.createDefaultMrg());
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateDnsZones()).thenReturn(mockPrivateDnsZones);

    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockPrivateDnsZones).deleteById(resourceId);
  }

  private void setupArmManagersForDoStep(String dnsZoneName, TargetManagedResourceGroup mrg) {
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateDnsZones()).thenReturn(mockPrivateDnsZones);
    when(mockPrivateDnsZones.define(STORAGE_ACCOUNT_DNS_NAME)).thenReturn(mockDefine);
    when(mockDefine.withExistingResourceGroup(mrg.name())).thenReturn(mockWithCreate);
    when(mockWithCreate.withTags(tagsCaptor.capture())).thenReturn(mockWithCreate);
    when(mockWithCreate.create()).thenReturn(mockPrivateDnsZone);
  }
}
