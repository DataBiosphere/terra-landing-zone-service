package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.securityinsights.SecurityInsightsManager;
import com.azure.resourcemanager.securityinsights.models.AutomationRule;
import com.azure.resourcemanager.securityinsights.models.AutomationRules;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateSentinelRunPlaybookAutomationRuleTest extends BaseStepTest {

  @Mock LandingZoneProtectedDataConfiguration mockLandingZoneProtectedDataConfiguration;
  @Mock SecurityInsightsManager mockSecurityInsightsManager;
  @Mock AutomationRules mockAutomationRules;
  @Mock AutomationRule.DefinitionStages.Blank mockAutomationRuleDefinitionStageBlank;

  @Mock
  AutomationRule.DefinitionStages.WithDisplayName mockAutomationRuleDefinitionStageWithDisplayName;

  @Mock AutomationRule.DefinitionStages.WithOrder mockAutomationRuleDefinitionStageWithOrder;

  @Mock
  AutomationRule.DefinitionStages.WithTriggeringLogic
      mockAutomationRuleDefinitionStageWithTriggerLogic;

  @Mock AutomationRule.DefinitionStages.WithActions mockAutomationRuleDefinitionStageWithActions;
  @Mock AutomationRule.DefinitionStages.WithCreate mockAutomationRuleDefinitionStageWithCreate;
  @Mock AutomationRule mockAutomationRule;

  private CreateSentinelRunPlaybookAutomationRule createSentinelRunPlaybookAutomationRule;

  @BeforeEach
  void setUp() {
    createSentinelRunPlaybookAutomationRule =
        new CreateSentinelRunPlaybookAutomationRule(
            mockArmManagers,
            mockParametersResolver,
            mockResourceNameGenerator,
            mockLandingZoneProtectedDataConfiguration);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            new TargetManagedResourceGroup("mgrName", "mrgRegion"),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            buildLandingZoneResource()));
    setupArmManagersForDoStep("ruleId");

    when(mockLandingZoneProtectedDataConfiguration.getLogicAppResourceId())
        .thenReturn("logicAppResourceId");
    when(mockLandingZoneProtectedDataConfiguration.getTenantId())
        .thenReturn(UUID.randomUUID().toString());

    StepResult stepResult = createSentinelRunPlaybookAutomationRule.doStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockAutomationRuleDefinitionStageWithCreate, times(1)).create();
  }

  @Test
  void doStepMissingInputParameterThrowsException() {
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of());

    // missing LOG_ANALYTICS_RESOURCE_KEY in workingMap
    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createSentinelRunPlaybookAutomationRule.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    verifyUndoStep();
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    verifyUndoStep();
  }

  void verifyUndoStep() throws InterruptedException {
    // deleteResource do nothing
    assertThat(
        createSentinelRunPlaybookAutomationRule.undoStep(mockFlightContext),
        equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep(String ruleId) {
    when(mockAutomationRule.id()).thenReturn(ruleId);
    when(mockAutomationRuleDefinitionStageWithCreate.create()).thenReturn(mockAutomationRule);
    when(mockAutomationRuleDefinitionStageWithActions.withActions(any()))
        .thenReturn(mockAutomationRuleDefinitionStageWithCreate);
    when(mockAutomationRuleDefinitionStageWithTriggerLogic.withTriggeringLogic(any()))
        .thenReturn(mockAutomationRuleDefinitionStageWithActions);
    when(mockAutomationRuleDefinitionStageWithOrder.withOrder(anyInt()))
        .thenReturn(mockAutomationRuleDefinitionStageWithTriggerLogic);
    when(mockAutomationRuleDefinitionStageWithDisplayName.withDisplayName(anyString()))
        .thenReturn(mockAutomationRuleDefinitionStageWithOrder);
    when(mockAutomationRuleDefinitionStageBlank.withExistingWorkspace(anyString(), anyString()))
        .thenReturn(mockAutomationRuleDefinitionStageWithDisplayName);
    when(mockAutomationRules.define(anyString()))
        .thenReturn(mockAutomationRuleDefinitionStageBlank);
    when(mockSecurityInsightsManager.automationRules()).thenReturn(mockAutomationRules);
    when(mockArmManagers.securityInsightsManager()).thenReturn(mockSecurityInsightsManager);
  }

  private void setupArmManagersForUndoStep(String ruleId) {
    when(mockSecurityInsightsManager.automationRules()).thenReturn(mockAutomationRules);
    when(mockArmManagers.securityInsightsManager()).thenReturn(mockSecurityInsightsManager);
  }
}
