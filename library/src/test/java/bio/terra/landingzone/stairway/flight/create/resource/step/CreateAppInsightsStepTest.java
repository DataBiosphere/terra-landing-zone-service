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
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import com.azure.resourcemanager.applicationinsights.models.Components;
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
class CreateAppInsightsStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "appInsightsId";

  @Mock private ApplicationInsightsManager mockApplicationInsightsManager;
  @Mock private Components mockComponents;

  @Mock
  private ApplicationInsightsComponent.DefinitionStages.Blank mockAppInsightDefinitionStagesBlank;

  @Mock
  private ApplicationInsightsComponent.DefinitionStages.WithResourceGroup
      mockAppInsightDefinitionStagesWithResourceGroup;

  @Mock
  private ApplicationInsightsComponent.DefinitionStages.WithKind
      mockAppInsightDefinitionStagesWithKind;

  @Mock
  private ApplicationInsightsComponent.DefinitionStages.WithCreate
      mockAppInsightDefinitionStagesWithCreate;

  @Mock private ApplicationInsightsComponent mockApplicationInsightsComponent;

  @Captor private ArgumentCaptor<String> kindCaptor;
  @Captor private ArgumentCaptor<ApplicationType> applicationTypeCaptor;
  @Captor private ArgumentCaptor<String> resourceGroupNameCaptor;
  @Captor private ArgumentCaptor<String> resourceGroupRegionCaptor;

  private CreateAppInsightsStep createAppInsightsStep;

  @BeforeEach
  void setup() {
    createAppInsightsStep =
        new CreateAppInsightsStep(
            mockArmManagers, mockParametersResolver, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String logAnalyticsWorkspaceId = "logAnalyticsWorkspaceId";
    final String appInsightName = "appInsightName";

    when(mockResourceNameProvider.getName(createAppInsightsStep.getResourceType()))
        .thenReturn(appInsightName);

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
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            logAnalyticsWorkspaceId));
    setupArmManagersForDoStep(RESOURCE_ID, appInsightName, logAnalyticsWorkspaceId);

    StepResult stepResult = createAppInsightsStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyBasicTags(tagsCaptor.getValue(), LANDING_ZONE_ID);
    verify(mockAppInsightDefinitionStagesWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockAppInsightDefinitionStagesWithCreate);
    assertThat(kindCaptor.getValue(), equalTo("java"));
    assertThat(applicationTypeCaptor.getValue(), equalTo(ApplicationType.OTHER));
    assertThat(resourceGroupNameCaptor.getValue(), equalTo(mrg.name()));
    assertThat(resourceGroupRegionCaptor.getValue(), equalTo(mrg.region()));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createAppInsightsStep.doStep(mockFlightContext));
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
                LANDING_ZONE_ID,
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
                ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()));
    FlightMap flightMapWorkingParameters =
        FlightTestUtils.prepareFlightWorkingParameters(workingParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);
    when(mockFlightContext.getWorkingMap()).thenReturn(flightMapWorkingParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createAppInsightsStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    workingMap.put(CreateAppInsightsStep.APP_INSIGHT_ID, RESOURCE_ID);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);
    when(mockApplicationInsightsManager.components()).thenReturn(mockComponents);
    when(mockArmManagers.applicationInsightsManager()).thenReturn(mockApplicationInsightsManager);

    var stepResult = createAppInsightsStep.undoStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockComponents, times(1)).deleteById(RESOURCE_ID);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    var workingMap = new FlightMap(); // empty, there is no APP_INSIGHT_ID key
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    var stepResult = createAppInsightsStep.undoStep(mockFlightContext);

    verify(mockComponents, never()).deleteById(anyString());
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep(
      String appInsightId, String appInsightName, String workspaceResourceId) {
    when(mockApplicationInsightsComponent.id()).thenReturn(appInsightId);
    when(mockAppInsightDefinitionStagesWithCreate.create())
        .thenReturn(mockApplicationInsightsComponent);
    when(mockAppInsightDefinitionStagesWithCreate.withTags(tagsCaptor.capture()))
        .thenReturn(mockAppInsightDefinitionStagesWithCreate);
    when(mockAppInsightDefinitionStagesWithCreate.withWorkspaceResourceId(workspaceResourceId))
        .thenReturn(mockAppInsightDefinitionStagesWithCreate);
    when(mockAppInsightDefinitionStagesWithCreate.withApplicationType(
            applicationTypeCaptor.capture()))
        .thenReturn(mockAppInsightDefinitionStagesWithCreate);
    when(mockAppInsightDefinitionStagesWithKind.withKind(kindCaptor.capture()))
        .thenReturn(mockAppInsightDefinitionStagesWithCreate);
    when(mockAppInsightDefinitionStagesWithResourceGroup.withExistingResourceGroup(
            resourceGroupNameCaptor.capture()))
        .thenReturn(mockAppInsightDefinitionStagesWithKind);
    when(mockAppInsightDefinitionStagesBlank.withRegion(resourceGroupRegionCaptor.capture()))
        .thenReturn(mockAppInsightDefinitionStagesWithResourceGroup);
    when(mockComponents.define(appInsightName)).thenReturn(mockAppInsightDefinitionStagesBlank);
    when(mockApplicationInsightsManager.components()).thenReturn(mockComponents);
    when(mockArmManagers.applicationInsightsManager()).thenReturn(mockApplicationInsightsManager);
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        // intentionally return empty map, to check required parameter validation
        Arguments.of(Map.of()));
  }
}
