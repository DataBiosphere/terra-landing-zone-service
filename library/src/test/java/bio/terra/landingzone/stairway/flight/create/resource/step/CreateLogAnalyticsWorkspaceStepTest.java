package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.loganalytics.models.Workspace;
import com.azure.resourcemanager.loganalytics.models.Workspaces;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.resources.models.GenericResources;
import java.util.ArrayList;
import java.util.List;
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
class CreateLogAnalyticsWorkspaceStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "logAnalyticsWorkspaceId";

  @Mock private LogAnalyticsManager mockLogAnalyticsManager;
  @Mock private Workspaces mockWorkspaces;
  @Mock private Workspace.DefinitionStages.Blank mockDefinitionStageBlank;
  @Mock private Workspace.DefinitionStages.WithResourceGroup mockDefinitionStageWithResourceGroup;
  @Mock private Workspace.DefinitionStages.WithCreate mockDefinitionStageWithCreate;
  @Mock private Workspace mockWorkspace;

  @Mock private GenericResources mockGenericResources;

  @Captor private ArgumentCaptor<String> regionCaptor;
  @Captor private ArgumentCaptor<String> resourceGroupNameCaptor;
  @Captor private ArgumentCaptor<Integer> retentionInDaysCaptor;
  @Captor private ArgumentCaptor<String> logAnalyticsWorkspaceNameCaptor;

  private CreateLogAnalyticsWorkspaceStep createLogAnalyticsWorkspaceStep;

  @BeforeEach
  void setup() {
    createLogAnalyticsWorkspaceStep =
        new CreateLogAnalyticsWorkspaceStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String logAnalyticsWorkspaceName = "logAnalyticsWorkspaceName";
    final int retentionInDays = 30;

    when(mockResourceNameProvider.getName(createLogAnalyticsWorkspaceStep.getResourceType()))
        .thenReturn(logAnalyticsWorkspaceName);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneDefaultParameters.ParametersNames.AUDIT_LOG_RETENTION_DAYS.name(),
            retentionInDays,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg));
    setupArmManagersForDoStep();

    StepResult stepResult = createLogAnalyticsWorkspaceStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyBasicTags(tagsCaptor.getValue(), LANDING_ZONE_ID);
    assertThat(regionCaptor.getValue(), equalTo(mrg.region()));
    assertThat(resourceGroupNameCaptor.getValue(), equalTo(mrg.name()));
    assertThat(retentionInDaysCaptor.getValue(), equalTo(retentionInDays));
    assertThat(logAnalyticsWorkspaceNameCaptor.getValue(), equalTo(logAnalyticsWorkspaceName));
    verify(mockDefinitionStageWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockDefinitionStageWithCreate);
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createLogAnalyticsWorkspaceStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    workingMap.put(CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID, RESOURCE_ID);
    workingMap.put(
        GetManagedResourceGroupInfo.TARGET_MRG_KEY, ResourceStepFixture.createDefaultMrg());
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    // mocking for logAnalyticsWorkspace deletion
    when(mockLogAnalyticsManager.workspaces()).thenReturn(mockWorkspaces);
    when(mockArmManagers.logAnalyticsManager()).thenReturn(mockLogAnalyticsManager);

    // mocking for ContainerInsight deletion
    final String containerInsightResourceId1 = "containerInsightResourceId1";
    final String containerInsightResourceId2 = "containerInsightResourceId2";
    PagedIterable<GenericResource> mockPagedIterableResult =
        mockContainerInsightListingResult(
            List.of(containerInsightResourceId1, containerInsightResourceId2));
    when(mockGenericResources.listByResourceGroup(any())).thenReturn(mockPagedIterableResult);
    when(mockAzureResourceManager.genericResources()).thenReturn(mockGenericResources);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);

    var stepResult = createLogAnalyticsWorkspaceStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockWorkspaces, times(1)).deleteById(RESOURCE_ID);
    verifyNoMoreInteractions(mockWorkspaces);
    verify(mockGenericResources, times(1)).deleteById(containerInsightResourceId1);
    verify(mockGenericResources, times(1)).deleteById(containerInsightResourceId2);
    verifyNoMoreInteractions(mockGenericResources);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    var workingMap = new FlightMap(); // empty, there is no LOG_ANALYTICS_WORKSPACE_ID key
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    var stepResult = createLogAnalyticsWorkspaceStep.undoStep(mockFlightContext);

    verify(mockWorkspaces, never()).deleteById(anyString());
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep() {
    when(mockWorkspace.id()).thenReturn(RESOURCE_ID);
    when(mockDefinitionStageWithCreate.create()).thenReturn(mockWorkspace);
    when(mockDefinitionStageWithCreate.withTags(tagsCaptor.capture()))
        .thenReturn(mockDefinitionStageWithCreate);
    when(mockDefinitionStageWithCreate.withRetentionInDays(retentionInDaysCaptor.capture()))
        .thenReturn(mockDefinitionStageWithCreate);
    when(mockDefinitionStageWithResourceGroup.withExistingResourceGroup(
            resourceGroupNameCaptor.capture()))
        .thenReturn(mockDefinitionStageWithCreate);
    when(mockDefinitionStageBlank.withRegion(regionCaptor.capture()))
        .thenReturn(mockDefinitionStageWithResourceGroup);
    when(mockWorkspaces.define(logAnalyticsWorkspaceNameCaptor.capture()))
        .thenReturn(mockDefinitionStageBlank);
    when(mockLogAnalyticsManager.workspaces()).thenReturn(mockWorkspaces);
    when(mockArmManagers.logAnalyticsManager()).thenReturn(mockLogAnalyticsManager);
  }

  private static PagedIterable<GenericResource> mockContainerInsightListingResult(
      List<String> containerInsightResourceIds) {
    PagedIterable<GenericResource> mockPagedIterableResult = mock(PagedIterable.class);
    List<GenericResource> genericResources = new ArrayList<>();
    for (var id : containerInsightResourceIds) {
      GenericResource resource = mock(GenericResource.class);
      when(resource.id()).thenReturn(id);
      when(resource.resourceProviderNamespace()).thenReturn("Microsoft.OperationsManagement");
      when(resource.resourceType()).thenReturn("solutions");
      genericResources.add(resource);
    }
    when(mockPagedIterableResult.stream()).thenReturn(genericResources.stream());
    return mockPagedIterableResult;
  }
}
