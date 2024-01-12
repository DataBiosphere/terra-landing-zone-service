package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.securityinsights.SecurityInsightsManager;
import com.azure.resourcemanager.securityinsights.models.SentinelOnboardingState;
import com.azure.resourcemanager.securityinsights.models.SentinelOnboardingStates;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateSentinelStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "sentinelId";

  @Mock SecurityInsightsManager mockSecurityInsightsManager;
  @Mock SentinelOnboardingStates mockSentinelOnboardingStages;
  @Mock SentinelOnboardingState.DefinitionStages.Blank mockDefinitionStagesBlank;
  @Mock SentinelOnboardingState.DefinitionStages.WithCreate mockDefinitionStagesWithCreate;
  @Mock SentinelOnboardingState mockSentinelOnboardingState;

  private CreateSentinelStep createSentinelStep;

  @Captor ArgumentCaptor<String> resourceGroupNameCaptor;
  @Captor ArgumentCaptor<String> workspaceNameCaptor;

  @BeforeEach
  void setup() {
    createSentinelStep = new CreateSentinelStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    var logAnalyticsLandingZoneResource = buildLandingZoneResource();
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
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            logAnalyticsLandingZoneResource));
    setupArmManagersForDoStep(RESOURCE_ID);

    StepResult stepResult = createSentinelStep.doStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    assertThat(
        workspaceNameCaptor.getValue(),
        equalTo(logAnalyticsLandingZoneResource.resourceName().get()));
    assertThat(resourceGroupNameCaptor.getValue(), equalTo(mrg.name()));
    verify(mockDefinitionStagesWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockDefinitionStagesWithCreate);
  }

  @Test
  void doStepMissingInputParameterThrowsException() {
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of());

    // missing LOG_ANALYTICS_RESOURCE_KEY in workingMap
    assertThrows(
        MissingRequiredFieldsException.class, () -> createSentinelStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    setupFlightContext(
        mockFlightContext, null, Map.of(CreateSentinelStep.SENTINEL_ID, RESOURCE_ID));
    setupArmManagersForUndoStep();

    StepResult stepResult = createSentinelStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockSentinelOnboardingStages, times(1)).deleteById(RESOURCE_ID);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    setupFlightContext(mockFlightContext, null, Map.of());

    var stepResult = createSentinelStep.undoStep(mockFlightContext);

    verify(mockSentinelOnboardingStages, never()).deleteById(anyString());
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep(String resourceId) {
    when(mockSentinelOnboardingState.id()).thenReturn(resourceId);
    when(mockDefinitionStagesWithCreate.create()).thenReturn(mockSentinelOnboardingState);
    when(mockDefinitionStagesBlank.withExistingWorkspace(
            resourceGroupNameCaptor.capture(), workspaceNameCaptor.capture()))
        .thenReturn(mockDefinitionStagesWithCreate);
    when(mockSentinelOnboardingStages.define(anyString())).thenReturn(mockDefinitionStagesBlank);
    when(mockSecurityInsightsManager.sentinelOnboardingStates())
        .thenReturn(mockSentinelOnboardingStages);
    when(mockArmManagers.securityInsightsManager()).thenReturn(mockSecurityInsightsManager);
  }

  private void setupArmManagersForUndoStep() {
    when(mockSecurityInsightsManager.sentinelOnboardingStates())
        .thenReturn(mockSentinelOnboardingStages);
    when(mockArmManagers.securityInsightsManager()).thenReturn(mockSecurityInsightsManager);
  }
}
