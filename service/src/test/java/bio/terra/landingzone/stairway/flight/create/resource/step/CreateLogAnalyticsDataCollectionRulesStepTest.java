package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.monitor.fluent.DataCollectionRulesClient;
import com.azure.resourcemanager.monitor.fluent.MonitorClient;
import com.azure.resourcemanager.monitor.fluent.models.DataCollectionRuleResourceInner;
import com.azure.resourcemanager.monitor.models.KnownDataFlowStreams;
import com.azure.resourcemanager.monitor.models.KnownPerfCounterDataSourceStreams;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceFacilityNames;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceLogLevels;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceStreams;
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
class CreateLogAnalyticsDataCollectionRulesStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "dataCollectionRuleId";

  @Mock private MonitorManager mockMonitorManager;
  @Mock private MonitorClient mockServiceClient;
  @Mock private DataCollectionRulesClient mockDataCollectionRulesClient;
  @Mock private Response<DataCollectionRuleResourceInner> mockResponse;
  @Mock private DataCollectionRuleResourceInner mockDataCollectionRuleResourceInner;

  @Captor private ArgumentCaptor<String> resourceGroupNameCaptor;
  @Captor private ArgumentCaptor<String> dataCollectionRuleNameCaptor;

  @Captor
  private ArgumentCaptor<DataCollectionRuleResourceInner> dataCollectionRuleResourceInnerCaptor;

  @Captor private ArgumentCaptor<Context> contextCaptor;

  private CreateLogAnalyticsDataCollectionRulesStep createLogAnalyticsDataCollectionRulesStep;

  @BeforeEach
  void setup() {
    createLogAnalyticsDataCollectionRulesStep =
        new CreateLogAnalyticsDataCollectionRulesStep(
            mockArmManagers, mockParametersResolver, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String logAnalyticsWorkspaceId = "logAnalyticsWorkspaceId";
    final String dataCollectionRuleName = "storageAuditLogSettingsName";

    when(mockResourceNameProvider.getName(
            createLogAnalyticsDataCollectionRulesStep.getResourceType()))
        .thenReturn(dataCollectionRuleName);

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
            logAnalyticsWorkspaceId));
    setupArmManagersForDoStep();

    StepResult stepResult = createLogAnalyticsDataCollectionRulesStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockDataCollectionRulesClient, times(1))
        .createWithResponse(
            anyString(),
            anyString(),
            any(DataCollectionRuleResourceInner.class),
            any(Context.class));

    assertThat(resourceGroupNameCaptor.getValue(), equalTo(mrg.name()));
    assertThat(dataCollectionRuleNameCaptor.getValue(), equalTo(dataCollectionRuleName));
    validateCreateRequest(
        dataCollectionRuleResourceInnerCaptor.getValue(), logAnalyticsWorkspaceId, mrg.region());
    assertThat(contextCaptor.getValue(), equalTo(Context.NONE));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createLogAnalyticsDataCollectionRulesStep.doStep(mockFlightContext));
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
        () -> createLogAnalyticsDataCollectionRulesStep.doStep(mockFlightContext));
  }

  private void setupArmManagersForDoStep() {
    when(mockDataCollectionRuleResourceInner.id()).thenReturn(RESOURCE_ID);
    when(mockResponse.getValue()).thenReturn(mockDataCollectionRuleResourceInner);
    when(mockDataCollectionRulesClient.createWithResponse(
            resourceGroupNameCaptor.capture(),
            dataCollectionRuleNameCaptor.capture(),
            dataCollectionRuleResourceInnerCaptor.capture(),
            contextCaptor.capture()))
        .thenReturn(mockResponse);
    when(mockServiceClient.getDataCollectionRules()).thenReturn(mockDataCollectionRulesClient);
    when(mockMonitorManager.serviceClient()).thenReturn(mockServiceClient);
    when(mockArmManagers.monitorManager()).thenReturn(mockMonitorManager);
  }

  private void validateCreateRequest(
      DataCollectionRuleResourceInner dataCollectionRuleResourceInner,
      String logAnalyticsWorkspaceId,
      String resourceGroupRegion) {
    final String destination = "lz_workspace";

    assertNotNull(dataCollectionRuleResourceInner);
    var dataSource = dataCollectionRuleResourceInner.dataSources();
    assertNotNull(dataSource);
    assertNotNull(dataSource.performanceCounters());
    assertThat(dataSource.performanceCounters().size(), equalTo(1));
    assertTrue(
        dataSource.performanceCounters().stream()
            .anyMatch(
                e ->
                    e.name().equals("VMInsightsPerfCounters")
                        && e.counterSpecifiers().contains("\\VmInsights\\DetailedMetrics")
                        && e.streams()
                            .contains(KnownPerfCounterDataSourceStreams.MICROSOFT_INSIGHTS_METRICS)
                        && e.samplingFrequencyInSeconds().equals(60)));

    var dataSourceSyslog = dataSource.syslog();
    assertNotNull(dataSourceSyslog);
    assertThat(dataSourceSyslog.size(), equalTo(1));
    assertTrue(
        dataSourceSyslog.stream()
            .anyMatch(
                e ->
                    e.name().equals("syslog")
                        && e.facilityNames().contains(KnownSyslogDataSourceFacilityNames.ASTERISK)
                        && e.logLevels().contains(KnownSyslogDataSourceLogLevels.ASTERISK)
                        && e.streams().contains(KnownSyslogDataSourceStreams.MICROSOFT_SYSLOG)));

    var destinations = dataCollectionRuleResourceInner.destinations();
    assertNotNull(destinations);
    assertNotNull(destinations.logAnalytics());
    assertThat(destinations.logAnalytics().size(), equalTo(1));
    assertTrue(
        destinations.logAnalytics().stream()
            .anyMatch(
                e ->
                    e.name().equals(destination)
                        && e.workspaceResourceId().equals(logAnalyticsWorkspaceId)));

    var dataFlows = dataCollectionRuleResourceInner.dataFlows();
    assertNotNull(dataFlows);
    assertThat(dataFlows.size(), equalTo(2));
    assertTrue(
        dataFlows.stream()
            .anyMatch(
                e ->
                    e.streams().contains(KnownDataFlowStreams.MICROSOFT_PERF)
                        && e.destinations().contains(destination)));
    assertTrue(
        dataFlows.stream()
            .anyMatch(
                e ->
                    e.streams().contains(KnownDataFlowStreams.MICROSOFT_SYSLOG)
                        && e.destinations().contains(destination)));

    assertThat(dataCollectionRuleResourceInner.location(), equalTo(resourceGroupRegion));
    verifyBasicTags(dataCollectionRuleResourceInner.tags(), LANDING_ZONE_ID);
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        // intentionally return empty map, to check required parameter validation
        Arguments.of(Map.of()));
  }
}
