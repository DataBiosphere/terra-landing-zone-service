package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.monitor.models.DiagnosticSetting;
import com.azure.resourcemanager.monitor.models.DiagnosticSettings;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateBatchLogSettingsStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "batchLogSettingId";

  @Mock private MonitorManager mockMonitorManager;
  @Mock private DiagnosticSettings mockDiagnosticSettings;
  @Mock private DiagnosticSetting.DefinitionStages.Blank mockDefinitionStagesBlank;

  @Mock
  private DiagnosticSetting.DefinitionStages.WithDiagnosticLogRecipient
      mockWithDiagnosticLogRecipient;

  @Mock private DiagnosticSetting.DefinitionStages.WithCreate mockWithCreate;
  @Mock private DiagnosticSetting mockDiagnosticSetting;

  @Captor private ArgumentCaptor<String> logAnalyticsWorkspaceIdCaptor;
  @Captor private ArgumentCaptor<String> batchAccountIdCaptor;
  @Captor private ArgumentCaptor<String> batchLogSettingsNameCaptor;

  private CreateBatchLogSettingsStep createBatchLogSettingsStep;

  @BeforeEach
  void setup() {
    createBatchLogSettingsStep =
        new CreateBatchLogSettingsStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String logAnalyticsWorkspaceId = "logAnalyticsWorkspaceId";
    final String batchAccountId = "batchAccountId";
    final String batchLogSettingName = "batchLogSettingName";

    when(mockResourceNameProvider.getName(createBatchLogSettingsStep.getResourceType()))
        .thenReturn(batchLogSettingName);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
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
            mrg,
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            logAnalyticsWorkspaceId,
            CreateBatchAccountStep.BATCH_ACCOUNT_ID,
            batchAccountId));
    setupArmManagersForDoStep();

    StepResult stepResult = createBatchLogSettingsStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockWithCreate, times(1)).create();
    assertThat(logAnalyticsWorkspaceIdCaptor.getValue(), equalTo(logAnalyticsWorkspaceId));
    assertThat(batchAccountIdCaptor.getValue(), equalTo(batchAccountId));
    assertThat(batchLogSettingsNameCaptor.getValue(), equalTo(batchLogSettingName));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    setupFlightContext(mockFlightContext, inputParameters, Map.of());

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createBatchLogSettingsStep.doStep(mockFlightContext));
  }

  @ParameterizedTest
  @MethodSource("workingParametersProvider")
  void doStepMissingWorkingParameterThrowsException(Map<String, Object> workingParameters) {
    var inputParameters =
        Map.of(
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone());
    setupFlightContext(mockFlightContext, inputParameters, workingParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createBatchLogSettingsStep.doStep(mockFlightContext));
  }

  private void setupArmManagersForDoStep() {
    when(mockDiagnosticSetting.id()).thenReturn(RESOURCE_ID);
    when(mockWithCreate.create()).thenReturn(mockDiagnosticSetting);
    when(mockWithCreate.withLog("ServiceLogs", 0)).thenReturn(mockWithCreate);
    when(mockWithCreate.withLog("ServiceLog", 0)).thenReturn(mockWithCreate);
    when(mockWithCreate.withLog("AuditLog", 0)).thenReturn(mockWithCreate);
    when(mockWithDiagnosticLogRecipient.withLogAnalytics(logAnalyticsWorkspaceIdCaptor.capture()))
        .thenReturn(mockWithCreate);
    when(mockDefinitionStagesBlank.withResource(batchAccountIdCaptor.capture()))
        .thenReturn(mockWithDiagnosticLogRecipient);
    when(mockDiagnosticSettings.define(batchLogSettingsNameCaptor.capture()))
        .thenReturn(mockDefinitionStagesBlank);
    when(mockMonitorManager.diagnosticSettings()).thenReturn(mockDiagnosticSettings);
    when(mockArmManagers.monitorManager()).thenReturn(mockMonitorManager);
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        Arguments.of(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()),
                CreateBatchAccountStep.BATCH_ACCOUNT_ID,
                "batchAccountId")),
        Arguments.of(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()),
                CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
                "logAnalyticsWorkspaceId")));
  }
}
