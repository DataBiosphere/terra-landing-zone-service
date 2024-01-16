package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.relay.RelayManager;
import com.azure.resourcemanager.relay.models.Namespaces;
import com.azure.resourcemanager.relay.models.RelayNamespace;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateRelayNamespaceStepTest extends BaseStepTest {
  private static final String RELAY_NAMESPACE_NAME = "testRelayNamespaceName";
  private static final String RELAY_NAMESPACE_ID = "relayNamespaceId";

  @Mock private RelayManager mockRelayManager;
  @Mock private Namespaces mockNamespaces;
  @Mock private RelayNamespace.DefinitionStages.Blank mockRelayNamespaceDefinitionStagesBlank;

  @Mock
  private RelayNamespace.DefinitionStages.WithResourceGroup
      mockRelayNamespaceDefinitionStagesWithResourceGroup;

  @Mock
  private RelayNamespace.DefinitionStages.WithCreate mockRelayNamespaceDefinitionStagesWithCreate;

  @Mock private RelayNamespace mockRelayNamespace;

  @Captor private ArgumentCaptor<Map<String, String>> relayNamespaceTagsCaptor;

  private CreateRelayNamespaceStep createRelayNamespaceStep;

  @BeforeEach
  void setup() {
    createRelayNamespaceStep = new CreateRelayNamespaceStep(mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(createRelayNamespaceStep.getResourceType()))
        .thenReturn(RELAY_NAMESPACE_NAME);

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
    setupArmManagersForDoStep(RELAY_NAMESPACE_ID, RELAY_NAMESPACE_NAME, mrg.region(), mrg.name());

    StepResult stepResult = createRelayNamespaceStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyBasicTags(relayNamespaceTagsCaptor.getValue(), LANDING_ZONE_ID);
    verify(mockRelayNamespaceDefinitionStagesWithCreate, times(1)).create();
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    setupFlightContext(mockFlightContext, inputParameters, Map.of());
    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createRelayNamespaceStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    when(mockRelayManager.namespaces()).thenReturn(mockNamespaces);
    when(mockArmManagers.relayManager()).thenReturn(mockRelayManager);
    setupFlightContext(
        mockFlightContext,
        Map.of(),
        Map.of(CreateRelayNamespaceStep.RELAY_NAMESPACE_ID, RELAY_NAMESPACE_ID));

    var stepResult = createRelayNamespaceStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockNamespaces, times(1)).deleteById(RELAY_NAMESPACE_ID);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    setupFlightContext(
        mockFlightContext, Map.of(), Map.of() // empty, there is no RELAY_NAMESPACE_ID key
        );

    var stepResult = createRelayNamespaceStep.undoStep(mockFlightContext);

    verify(mockNamespaces, never()).deleteById(anyString());
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep(
      String relayNamespaceId, String name, String region, String resourceGroup) {
    when(mockRelayNamespace.id()).thenReturn(relayNamespaceId);
    when(mockRelayNamespaceDefinitionStagesWithCreate.create()).thenReturn(mockRelayNamespace);
    when(mockRelayNamespaceDefinitionStagesWithCreate.withTags(relayNamespaceTagsCaptor.capture()))
        .thenReturn(mockRelayNamespaceDefinitionStagesWithCreate);
    when(mockRelayNamespaceDefinitionStagesWithResourceGroup.withExistingResourceGroup(
            resourceGroup))
        .thenReturn(mockRelayNamespaceDefinitionStagesWithCreate);
    when(mockRelayNamespaceDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockRelayNamespaceDefinitionStagesWithResourceGroup);
    when(mockNamespaces.define(name)).thenReturn(mockRelayNamespaceDefinitionStagesBlank);
    when(mockRelayManager.namespaces()).thenReturn(mockNamespaces);
    when(mockArmManagers.relayManager()).thenReturn(mockRelayManager);
  }
}
