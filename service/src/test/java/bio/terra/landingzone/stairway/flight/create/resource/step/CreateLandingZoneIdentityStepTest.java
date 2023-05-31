package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.msi.models.Identities;
import com.azure.resourcemanager.msi.models.Identity;
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
public class CreateLandingZoneIdentityStepTest extends BaseStepTest {
  private CreateLandingZoneIdentityStep testStep;
  @Mock private Identities mockIdentities;
  @Mock private Identity.DefinitionStages.Blank mockDefine;
  @Mock private Identity.DefinitionStages.WithGroup mockWithGroup;
  @Mock private Identity.DefinitionStages.WithCreate mockWithCreate;
  @Mock private Identity mockIdentity;

  @BeforeEach
  void setup() {
    testStep =
        new CreateLandingZoneIdentityStep(
            mockArmManagers, mockParametersResolver, mockResourceNameGenerator);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String uamiName = "uami-name";

    when(mockResourceNameGenerator.nextName(ResourceNameGenerator.UAMI_NAME_LENGTH))
        .thenReturn(uamiName);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg));
    setupArmManagersForDoStep(uamiName, mrg);

    var principalId = UUID.randomUUID().toString();
    var uamiId = UUID.randomUUID().toString();
    when(mockIdentity.id()).thenReturn(uamiId);
    when(mockIdentity.principalId()).thenReturn(principalId);

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_PRINCIPAL_ID, String.class),
        equalTo(principalId));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_ID, String.class),
        equalTo(uamiId));

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
    workingMap.put(CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_ID, resourceId);
    workingMap.put(
        GetManagedResourceGroupInfo.TARGET_MRG_KEY, ResourceStepFixture.createDefaultMrg());
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.identities()).thenReturn(mockIdentities);

    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockIdentities).deleteById(resourceId);
  }

  private void setupArmManagersForDoStep(String uamiName, TargetManagedResourceGroup mrg) {
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.identities()).thenReturn(mockIdentities);
    when(mockIdentities.define(uamiName)).thenReturn(mockDefine);
    when(mockDefine.withRegion(mrg.region())).thenReturn(mockWithGroup);
    when(mockWithGroup.withExistingResourceGroup(mrg.name())).thenReturn(mockWithCreate);
    when(mockWithCreate.withTags(tagsCaptor.capture())).thenReturn(mockWithCreate);
    when(mockWithCreate.create()).thenReturn(mockIdentity);
  }
}
