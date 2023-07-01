package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.Response;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.util.Context;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.monitor.fluent.DataCollectionRuleAssociationsClient;
import com.azure.resourcemanager.monitor.fluent.DataCollectionRulesClient;
import com.azure.resourcemanager.monitor.fluent.MonitorClient;
import com.azure.resourcemanager.monitor.fluent.models.DataCollectionRuleAssociationProxyOnlyResourceInner;
import com.azure.resourcemanager.monitor.fluent.models.DataCollectionRuleResourceInner;
import com.azure.resourcemanager.monitor.models.KnownDataFlowStreams;
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
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateAksCostOptimizationDataCollectionRulesStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "aksCostOptimizationDataCollectionRuleId";
  private static final String RULE_ASSOCIATION_ID = "ruleAssociationId";

  private static final String LOG_ANALYTICS_WORKSPACE_ID = "logAnalyticsWorkspaceId";
  private static final String AKS_ID = "aksId";
  private static final String DATA_COLLECTION_RULE_NAME = "aksCostOptimizationDataCollectionRule";

  @Mock private MonitorManager mockMonitorManager;
  @Mock private MonitorClient mockServiceClient;
  @Mock private DataCollectionRulesClient mockDataCollectionRulesClient;
  @Mock private DataCollectionRuleAssociationsClient mockDataCollectionRuleAssociationsClient;
  @Mock private Response<DataCollectionRuleResourceInner> mockResponse;

  @Mock
  private Response<DataCollectionRuleAssociationProxyOnlyResourceInner>
      mockRuleAssociationsResponse;

  @Mock private DataCollectionRuleResourceInner mockDataCollectionRuleResourceInner;

  @Mock
  private DataCollectionRuleAssociationProxyOnlyResourceInner
      mockDataCollectionRuleAssociationProxyOnlyResourceInner;

  @Captor private ArgumentCaptor<String> resourceGroupNameCaptor;
  @Captor private ArgumentCaptor<String> dataCollectionRuleNameCaptor;

  @Captor
  private ArgumentCaptor<DataCollectionRuleResourceInner> dataCollectionRuleResourceInnerCaptor;

  @Captor
  private ArgumentCaptor<DataCollectionRuleAssociationProxyOnlyResourceInner>
      dataCollectionRuleAssociationProxyOnlyResourceInnerCaptor;

  @Captor private ArgumentCaptor<String> resourceUriCaptor;
  @Captor private ArgumentCaptor<String> associationNameCaptor;

  @Captor private ArgumentCaptor<Context> contextCaptor;

  private CreateAksCostOptimizationDataCollectionRulesStep testStep;

  @BeforeEach
  void setup() {
    testStep =
        new CreateAksCostOptimizationDataCollectionRulesStep(
            mockArmManagers, mockParametersResolver, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    when(mockResourceNameProvider.getName(testStep.getResourceType()))
        .thenReturn(DATA_COLLECTION_RULE_NAME);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContextForSuccessfulFlight(mrg);
    setupArmManagersForDoStep();

    StepResult stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockDataCollectionRulesClient, times(1))
        .createWithResponse(
            anyString(),
            anyString(),
            any(DataCollectionRuleResourceInner.class),
            any(Context.class));
    verify(mockDataCollectionRuleAssociationsClient, times(1))
        .createWithResponse(
            anyString(),
            anyString(),
            any(DataCollectionRuleAssociationProxyOnlyResourceInner.class),
            any(Context.class));

    assertThat(resourceGroupNameCaptor.getValue(), equalTo(mrg.name()));
    assertThat(dataCollectionRuleNameCaptor.getValue(), equalTo(DATA_COLLECTION_RULE_NAME));
    assertThat(contextCaptor.getAllValues().get(0), equalTo(Context.NONE));
    validateCreateRuleRequest(dataCollectionRuleResourceInnerCaptor.getValue(), mrg.region());

    assertThat(resourceUriCaptor.getValue(), equalTo(AKS_ID));
    assertThat(associationNameCaptor.getValue(), equalTo("ContainerInsightsExtension"));
    validateCreateRuleAssociationRequest(
        dataCollectionRuleAssociationProxyOnlyResourceInnerCaptor.getValue());
    assertThat(contextCaptor.getAllValues().get(1), equalTo(Context.NONE));
  }

  @Test
  void doStepWhenRuleAlreadyExistsSuccess() throws InterruptedException {
    when(mockResourceNameProvider.getName(testStep.getResourceType()))
        .thenReturn(DATA_COLLECTION_RULE_NAME);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContextForSuccessfulFlight(mrg);
    setupArmManagersWhenRuleExists();

    StepResult stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockDataCollectionRulesClient, times(1))
        .createWithResponse(
            anyString(),
            anyString(),
            any(DataCollectionRuleResourceInner.class),
            any(Context.class));
    verify(mockDataCollectionRulesClient, times(1))
        .getByResourceGroup(mrg.name(), DATA_COLLECTION_RULE_NAME);
    verify(mockDataCollectionRuleAssociationsClient, times(1))
        .createWithResponse(
            anyString(),
            anyString(),
            any(DataCollectionRuleAssociationProxyOnlyResourceInner.class),
            any(Context.class));

    assertThat(resourceGroupNameCaptor.getValue(), equalTo(mrg.name()));
    assertThat(dataCollectionRuleNameCaptor.getValue(), equalTo(DATA_COLLECTION_RULE_NAME));
    assertThat(contextCaptor.getAllValues().get(0), equalTo(Context.NONE));
    validateCreateRuleRequest(dataCollectionRuleResourceInnerCaptor.getValue(), mrg.region());

    assertThat(resourceUriCaptor.getValue(), equalTo(AKS_ID));
    assertThat(associationNameCaptor.getValue(), equalTo("ContainerInsightsExtension"));
    validateCreateRuleAssociationRequest(
        dataCollectionRuleAssociationProxyOnlyResourceInnerCaptor.getValue());
    assertThat(contextCaptor.getAllValues().get(1), equalTo(Context.NONE));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
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

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  private void setupFlightContextForSuccessfulFlight(TargetManagedResourceGroup mrg) {
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
            LOG_ANALYTICS_WORKSPACE_ID,
            CreateAksStep.AKS_ID,
            AKS_ID));
  }

  private void setupArmManagersForDoStep() {
    // mocks for the rule
    when(mockDataCollectionRuleResourceInner.id()).thenReturn(RESOURCE_ID);
    when(mockResponse.getValue()).thenReturn(mockDataCollectionRuleResourceInner);
    when(mockDataCollectionRulesClient.createWithResponse(
            resourceGroupNameCaptor.capture(),
            dataCollectionRuleNameCaptor.capture(),
            dataCollectionRuleResourceInnerCaptor.capture(),
            contextCaptor.capture()))
        .thenReturn(mockResponse);
    when(mockServiceClient.getDataCollectionRules()).thenReturn(mockDataCollectionRulesClient);

    setupRuleAssociationMocks();
    setupBaseMocks();
  }

  private void setupArmManagersWhenRuleExists() {
    ManagementException mockManagementException = mock(ManagementException.class);
    HttpResponse mockHttpResponse = mock(HttpResponse.class);
    when(mockHttpResponse.getStatusCode()).thenReturn(HttpStatus.CONFLICT.value());
    when(mockManagementException.getResponse()).thenReturn(mockHttpResponse);

    // mocks for the rule
    // throw exception as rule already exists
    doThrow(mockManagementException)
        .when(mockDataCollectionRulesClient)
        .createWithResponse(
            resourceGroupNameCaptor.capture(),
            dataCollectionRuleNameCaptor.capture(),
            dataCollectionRuleResourceInnerCaptor.capture(),
            contextCaptor.capture());
    when(mockDataCollectionRuleResourceInner.id()).thenReturn(RESOURCE_ID);
    when(mockDataCollectionRulesClient.getByResourceGroup(
            resourceGroupNameCaptor.capture(), dataCollectionRuleNameCaptor.capture()))
        .thenReturn(mockDataCollectionRuleResourceInner);
    when(mockServiceClient.getDataCollectionRules()).thenReturn(mockDataCollectionRulesClient);

    setupRuleAssociationMocks();
    setupBaseMocks();
  }

  private void setupRuleAssociationMocks() {
    when(mockDataCollectionRuleAssociationProxyOnlyResourceInner.id())
        .thenReturn(RULE_ASSOCIATION_ID);
    when(mockRuleAssociationsResponse.getValue())
        .thenReturn(mockDataCollectionRuleAssociationProxyOnlyResourceInner);
    when(mockDataCollectionRuleAssociationsClient.createWithResponse(
            resourceUriCaptor.capture(),
            associationNameCaptor.capture(),
            dataCollectionRuleAssociationProxyOnlyResourceInnerCaptor.capture(),
            contextCaptor.capture()))
        .thenReturn(mockRuleAssociationsResponse);
    when(mockServiceClient.getDataCollectionRuleAssociations())
        .thenReturn(mockDataCollectionRuleAssociationsClient);
  }

  private void setupBaseMocks() {
    when(mockMonitorManager.serviceClient()).thenReturn(mockServiceClient);
    when(mockArmManagers.monitorManager()).thenReturn(mockMonitorManager);
  }

  private void validateCreateRuleRequest(
      DataCollectionRuleResourceInner dataCollectionRuleResourceInner, String resourceGroupRegion) {
    final String destination = "lz_workspace";

    assertNotNull(dataCollectionRuleResourceInner);
    var dataSource = dataCollectionRuleResourceInner.dataSources();
    assertNotNull(dataSource);
    assertNotNull(dataSource.extensions());
    assertThat(dataSource.extensions().size(), equalTo(1));
    var extensions =
        dataSource.extensions().stream()
            .filter(
                e ->
                    e.name().equals("ContainerInsightsExtension")
                        && e.extensionName().equals("ContainerInsights"))
            .toList();
    assertThat(extensions.size(), equalTo(1));
    var extensionSettings =
        (CreateAksCostOptimizationDataCollectionRulesStep.ExtensionSettings)
            extensions.get(0).extensionSettings();
    assertNotNull(extensionSettings);
    var dataCollectionSettingsSettings = extensionSettings.getDataCollectionSettings();
    assertNotNull(dataCollectionSettingsSettings);
    assertThat(
        dataCollectionSettingsSettings.getInterval(),
        equalTo(CreateAksCostOptimizationDataCollectionRulesStep.DATA_COLLECTION_INTERVAL));
    assertThat(
        dataCollectionSettingsSettings.getNamespaceFilteringMode(),
        equalTo(
            CreateAksCostOptimizationDataCollectionRulesStep
                .DATA_COLLECTION_NAMESPACE_FILTERING_MODE));

    var dataSourceSyslog = dataSource.syslog();
    assertNotNull(dataSourceSyslog);
    assertThat(dataSourceSyslog.size(), equalTo(1));
    assertTrue(
        dataSourceSyslog.stream()
            .anyMatch(
                e ->
                    e.name().equals("sysLogsDataSource")
                        /* excluding KnownSyslogDataSourceFacilityNames.ASTERISK*/
                        && e.facilityNames().size()
                            == (KnownSyslogDataSourceFacilityNames.values().size() - 1)
                        /*excluding KnownSyslogDataSourceLogLevels.ASTERISK*/
                        && e.logLevels().size()
                            == (KnownSyslogDataSourceLogLevels.values().size() - 1)
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
                        && e.workspaceResourceId().equals(LOG_ANALYTICS_WORKSPACE_ID)));

    var dataFlows = dataCollectionRuleResourceInner.dataFlows();
    assertNotNull(dataFlows);
    assertThat(dataFlows.size(), equalTo(1));
    assertTrue(
        dataFlows.stream()
            .anyMatch(
                e ->
                    e.streams().contains(KnownDataFlowStreams.MICROSOFT_INSIGHTS_METRICS)
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

  private void validateCreateRuleAssociationRequest(
      DataCollectionRuleAssociationProxyOnlyResourceInner
          dataCollectionRuleAssociationProxyOnlyResourceInner) {
    assertNotNull(dataCollectionRuleAssociationProxyOnlyResourceInner);
    assertThat(
        dataCollectionRuleAssociationProxyOnlyResourceInner.dataCollectionRuleId(),
        equalTo(RESOURCE_ID));
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        Arguments.of(Map.of(CreateAksStep.AKS_ID, "aksId")),
        Arguments.of(
            Map.of(
                CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
                "logAnalyticsWorkspaceId")));
  }
}
