package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.securityinsights.SecurityInsightsManager;
import com.azure.resourcemanager.securityinsights.models.AutomationRule;
import com.azure.resourcemanager.securityinsights.models.AutomationRuleAction;
import com.azure.resourcemanager.securityinsights.models.AutomationRuleRunPlaybookAction;
import com.azure.resourcemanager.securityinsights.models.AutomationRuleTriggeringLogic;
import com.azure.resourcemanager.securityinsights.models.AutomationRules;
import com.azure.resourcemanager.securityinsights.models.TriggersOn;
import com.azure.resourcemanager.securityinsights.models.TriggersWhen;
import java.util.List;
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

  @Captor ArgumentCaptor<AutomationRuleTriggeringLogic> automationRuleTriggeringLogicCaptor;
  @Captor ArgumentCaptor<List<AutomationRuleAction>> automationRuleRunPlaybookActionListCaptor;

  private CreateSentinelRunPlaybookAutomationRule createSentinelRunPlaybookAutomationRule;

  @BeforeEach
  void setup() {
    createSentinelRunPlaybookAutomationRule =
        new CreateSentinelRunPlaybookAutomationRule(mockResourceNameProvider, mockLandingZoneProtectedDataConfiguration);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    String logicAppResourceId = "logicAppResourceId";
    UUID tenantId = UUID.randomUUID();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            ResourceStepFixture.createDefaultMrg(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            buildLandingZoneResource()));
    setupArmManagersForDoStep("ruleId");

    when(mockLandingZoneProtectedDataConfiguration.getLogicAppResourceId())
        .thenReturn(logicAppResourceId);
    when(mockLandingZoneProtectedDataConfiguration.getTenantId()).thenReturn(tenantId.toString());

    StepResult stepResult = createSentinelRunPlaybookAutomationRule.doStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));

    assertNotNull(automationRuleTriggeringLogicCaptor.getValue());
    assertThat(
        automationRuleTriggeringLogicCaptor.getValue().triggersOn(), equalTo(TriggersOn.INCIDENTS));
    assertThat(
        automationRuleTriggeringLogicCaptor.getValue().triggersWhen(),
        equalTo(TriggersWhen.CREATED));
    assertThat(automationRuleTriggeringLogicCaptor.getValue().isEnabled(), equalTo(true));

    assertNotNull(automationRuleRunPlaybookActionListCaptor.getValue());
    assertThat(automationRuleRunPlaybookActionListCaptor.getValue().size(), equalTo(1));
    assertThat(
        automationRuleRunPlaybookActionListCaptor.getValue().get(0).getClass(),
        equalTo(AutomationRuleRunPlaybookAction.class));
    assertThat(automationRuleRunPlaybookActionListCaptor.getValue().get(0).order(), equalTo(1));
    AutomationRuleRunPlaybookAction playBookAction =
        (AutomationRuleRunPlaybookAction)
            (automationRuleRunPlaybookActionListCaptor.getValue().get(0));
    assertThat(
        playBookAction.actionConfiguration().logicAppResourceId(), equalTo(logicAppResourceId));
    assertThat(playBookAction.actionConfiguration().tenantId(), equalTo(tenantId));

    verify(mockAutomationRuleDefinitionStageWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockAutomationRuleDefinitionStageWithCreate);
  }

  @Test
  void doStepMissingInputParameterThrowsException() {
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(LandingZoneFlightMapKeys.BILLING_PROFILE, new ProfileModel().id(UUID.randomUUID())));

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
    when(mockAutomationRuleDefinitionStageWithActions.withActions(
            automationRuleRunPlaybookActionListCaptor.capture()))
        .thenReturn(mockAutomationRuleDefinitionStageWithCreate);
    when(mockAutomationRuleDefinitionStageWithTriggerLogic.withTriggeringLogic(
            automationRuleTriggeringLogicCaptor.capture()))
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
