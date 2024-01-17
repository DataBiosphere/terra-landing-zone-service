package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.monitor.models.DiagnosticSetting;
import com.azure.resourcemanager.monitor.models.DiagnosticSettings;
import java.time.Duration;
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
public class CreateAksLogSettingsStepTest extends BaseStepTest {
  @Mock private LandingZoneProtectedDataConfiguration mockLandingZoneProtectedDataConfiguration;
  @Mock private MonitorManager mockMonitorManager;
  @Mock private DiagnosticSettings mockDiagnosticSettings;
  @Mock private DiagnosticSetting.DefinitionStages.Blank mockDefinitionStagesBlank;

  @Mock
  private DiagnosticSetting.DefinitionStages.WithDiagnosticLogRecipient
      mockDefinitionStagesDiagnosticLogRecipient;

  @Mock private DiagnosticSetting.DefinitionStages.WithCreate mockDefinitionStagesCreate;
  @Mock private DiagnosticSetting mockDiagnosticSetting;

  @Captor private ArgumentCaptor<String> aksDiagnosticSettingsNameCaptor;
  @Captor private ArgumentCaptor<String> aksDiagnosticSettingsResourceIdCaptor;
  @Captor private ArgumentCaptor<String> aksDiagnosticSettingsStorageAccountIdCaptor;
  @Captor private ArgumentCaptor<String> aksLogNameCaptor;
  @Captor private ArgumentCaptor<Integer> aksLogRetentionCaptor;
  @Captor private ArgumentCaptor<String> aksMetricNameCaptor;
  @Captor private ArgumentCaptor<Duration> aksMetricGranularityCaptor;
  @Captor private ArgumentCaptor<Integer> aksMetricRetentionCaptor;

  private CreateAksLogSettingsStep createAksLogSettingsStep;

  @BeforeEach
  void setup() {
    createAksLogSettingsStep =
        new CreateAksLogSettingsStep(mockResourceNameProvider, mockLandingZoneProtectedDataConfiguration);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String aksDiagnosticSettingName = "aksDiagnosticSettingName";
    final String aksResourceId = "aksId";
    final Map<String, String> storageAccountIds = Map.of("eastus", "ltsAccountEastUs");

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(createAksLogSettingsStep.getResourceType()))
        .thenReturn(aksDiagnosticSettingName);
    when(mockLandingZoneProtectedDataConfiguration.getLongTermStorageAccountIds())
        .thenReturn(storageAccountIds);

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
            CreateAksStep.AKS_ID,
            aksResourceId,
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg));
    setupArmManagersForDoStep();

    var stepResult = createAksLogSettingsStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockDefinitionStagesCreate, times(1)).create();
    assertThat(aksDiagnosticSettingsNameCaptor.getValue(), equalTo(aksDiagnosticSettingName));
    assertThat(aksDiagnosticSettingsResourceIdCaptor.getValue(), equalTo(aksResourceId));
    assertThat(
        aksDiagnosticSettingsStorageAccountIdCaptor.getValue(),
        equalTo(storageAccountIds.get("eastus")));
    verifyDiagnosticConfiguration();
  }

  @Test
  void doStepLongTermStorageAccountNotFoundThrowsException() {
    final String aksResourceId = "aksId";
    final Map<String, String> storageAccountIds = Map.of("NOT_MATCHING_REGION", "ltsAccountEastUs");

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockLandingZoneProtectedDataConfiguration.getLongTermStorageAccountIds())
        .thenReturn(storageAccountIds);

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
            CreateAksStep.AKS_ID,
            aksResourceId,
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg));

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createAksLogSettingsStep.doStep(mockFlightContext));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    setupFlightContext(mockFlightContext, inputParameters, Map.of());

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createAksLogSettingsStep.doStep(mockFlightContext));
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
        () -> createAksLogSettingsStep.doStep(mockFlightContext));
  }

  private void setupArmManagersForDoStep() {
    when(mockDiagnosticSetting.id()).thenReturn("aksDiagnosticSettingId");
    when(mockDefinitionStagesCreate.create()).thenReturn(mockDiagnosticSetting);
    when(mockDefinitionStagesCreate.withMetric(
            aksMetricNameCaptor.capture(),
            aksMetricGranularityCaptor.capture(),
            aksMetricRetentionCaptor.capture()))
        .thenReturn(mockDefinitionStagesCreate);
    when(mockDefinitionStagesCreate.withLog(
            aksLogNameCaptor.capture(), aksLogRetentionCaptor.capture()))
        .thenReturn(mockDefinitionStagesCreate);
    when(mockDefinitionStagesDiagnosticLogRecipient.withStorageAccount(
            aksDiagnosticSettingsStorageAccountIdCaptor.capture()))
        .thenReturn(mockDefinitionStagesCreate);
    when(mockDefinitionStagesBlank.withResource(aksDiagnosticSettingsResourceIdCaptor.capture()))
        .thenReturn(mockDefinitionStagesDiagnosticLogRecipient);
    when(mockDiagnosticSettings.define(aksDiagnosticSettingsNameCaptor.capture()))
        .thenReturn(mockDefinitionStagesBlank);
    when(mockMonitorManager.diagnosticSettings()).thenReturn(mockDiagnosticSettings);
    when(mockArmManagers.monitorManager()).thenReturn(mockMonitorManager);
  }

  private void verifyDiagnosticConfiguration() {
    int numberOfLogsToCapture = 11;
    int numberOfMetricToCapture = 1;
    assertThat(aksLogNameCaptor.getAllValues().size(), equalTo(numberOfLogsToCapture));
    assertThat(aksLogRetentionCaptor.getAllValues().size(), equalTo(numberOfLogsToCapture));
    assertThat(aksMetricNameCaptor.getAllValues().size(), equalTo(numberOfMetricToCapture));
    assertThat(aksMetricGranularityCaptor.getAllValues().size(), equalTo(numberOfMetricToCapture));
    assertThat(aksMetricRetentionCaptor.getAllValues().size(), equalTo(numberOfMetricToCapture));
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        Arguments.of(
            Map.of(
                CreateAksStep.AKS_ID,
                "aksId",
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()))),
        Arguments.of(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()),
                GetManagedResourceGroupInfo.TARGET_MRG_KEY,
                ResourceStepFixture.createDefaultMrg())));
  }
}
