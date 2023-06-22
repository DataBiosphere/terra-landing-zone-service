package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
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
class CreatePostgresLogSettingsStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "postgresLogSettingId";

  @Mock private MonitorManager mockMonitorManager;
  @Mock private DiagnosticSettings mockDiagnosticSettings;
  @Mock private DiagnosticSetting.DefinitionStages.Blank mockWithBlank;

  @Mock
  private DiagnosticSetting.DefinitionStages.WithDiagnosticLogRecipient
      mockWithDiagnosticLogRecipient;

  @Mock private DiagnosticSetting.DefinitionStages.WithCreate mockWithCreate;
  @Mock private DiagnosticSetting mockDiagnosticSetting;

  @Captor private ArgumentCaptor<String> postgresLogSettingsNameCaptor;
  @Captor private ArgumentCaptor<String> postgreSqlIdCaptor;
  @Captor private ArgumentCaptor<String> logAnalyticsWorkspaceIdCaptor;

  private CreatePostgresLogSettingsStep createPostgresLogSettingsStep;

  @BeforeEach
  void setup() {
    createPostgresLogSettingsStep =
        new CreatePostgresLogSettingsStep(
            mockArmManagers, mockParametersResolver, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String logAnalyticsWorkspaceId = "logAnalyticsWorkspaceId";
    final String postgreSqlId = "postgreSqlId";
    final String postgresLogSettingsName = "postgresLogSettingsName";

    when(mockResourceNameProvider.getName(createPostgresLogSettingsStep.getResourceType()))
        .thenReturn(postgresLogSettingsName);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            logAnalyticsWorkspaceId,
            CreatePostgresqlDbStep.POSTGRESQL_ID,
            postgreSqlId));
    setupArmManagersForDoStep();

    StepResult stepResult = createPostgresLogSettingsStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockWithCreate);
    assertThat(logAnalyticsWorkspaceIdCaptor.getValue(), equalTo(logAnalyticsWorkspaceId));
    assertThat(postgreSqlIdCaptor.getValue(), equalTo(postgreSqlId));
    assertThat(postgresLogSettingsNameCaptor.getValue(), equalTo(postgresLogSettingsName));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createPostgresLogSettingsStep.doStep(mockFlightContext));
  }

  @ParameterizedTest
  @MethodSource("workingParametersProvider")
  void doStepMissingWorkingParameterThrowsException(Map<String, Object> workingParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()),
                LandingZoneFlightMapKeys.LANDING_ZONE_ID,
                LANDING_ZONE_ID));
    FlightMap flightMapWorkingParameters =
        FlightTestUtils.prepareFlightWorkingParameters(workingParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);
    when(mockFlightContext.getWorkingMap()).thenReturn(flightMapWorkingParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createPostgresLogSettingsStep.doStep(mockFlightContext));
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        Arguments.of(Map.of(CreatePostgresqlDbStep.POSTGRESQL_ID, "postgreSqlId")),
        Arguments.of(
            Map.of(
                CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
                "logAnalyticsWorkspaceId")));
  }

  private void setupArmManagersForDoStep() {
    when(mockDiagnosticSetting.id()).thenReturn(RESOURCE_ID);
    when(mockWithCreate.create()).thenReturn(mockDiagnosticSetting);
    when(mockWithCreate.withMetric("AllMetrics", Duration.ofMinutes(1), 0))
        .thenReturn(mockWithCreate);
    when(mockWithCreate.withLog("PostgreSQLLogs", 0)).thenReturn(mockWithCreate);
    when(mockWithCreate.withLog("PostgreSQLFlexQueryStoreRuntime", 0)).thenReturn(mockWithCreate);
    when(mockWithCreate.withLog("PostgreSQLFlexQueryStoreWaitStats", 0)).thenReturn(mockWithCreate);
    when(mockWithDiagnosticLogRecipient.withLogAnalytics(logAnalyticsWorkspaceIdCaptor.capture()))
        .thenReturn(mockWithCreate);
    when(mockWithBlank.withResource(postgreSqlIdCaptor.capture()))
        .thenReturn(mockWithDiagnosticLogRecipient);
    when(mockDiagnosticSettings.define(postgresLogSettingsNameCaptor.capture()))
        .thenReturn(mockWithBlank);
    when(mockMonitorManager.diagnosticSettings()).thenReturn(mockDiagnosticSettings);
    when(mockArmManagers.monitorManager()).thenReturn(mockMonitorManager);
  }
}
