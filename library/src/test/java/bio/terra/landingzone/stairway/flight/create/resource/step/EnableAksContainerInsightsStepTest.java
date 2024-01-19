package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.KubernetesClusters;
import com.azure.resourcemanager.containerservice.models.ManagedClusterAddonProfile;
import java.util.Map;
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
class EnableAksContainerInsightsStepTest extends BaseStepTest {
  EnableAksContainerInsightsStep testStep;

  @Mock KubernetesClusters mockKubernetesClusters;
  @Mock KubernetesCluster mockKubernetesCluster;
  @Mock KubernetesCluster.Update mockKubernetesClusterUpdate;

  @Captor ArgumentCaptor<String> aksIdCaptor;
  @Captor private ArgumentCaptor<Map<String, ManagedClusterAddonProfile>> addonProfileCaptor;

  @BeforeEach
  void setup() {
    testStep = new EnableAksContainerInsightsStep(mockArmManagers);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String aksId = "aksId";
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            CreateAksStep.AKS_ID,
            aksId,
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            "logAnalyticsWorkspaceId"));
    mockArmManagers();

    StepResult stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyAddonProfile(addonProfileCaptor.getValue());
  }

  @Test
  void doStepFailedWithRetryableError() throws InterruptedException {
    final String aksId = "aksId";
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            CreateAksStep.AKS_ID,
            aksId,
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            "logAnalyticsWorkspaceId"));
    mockArmManagers();

    var exception = mockRetryableException();
    doThrow(exception).when(mockKubernetesClusterUpdate).apply();

    StepResult stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_RETRY));
  }

  @Test
  void doStepFailedWithNonRetryableError() throws InterruptedException {
    final String aksId = "aksId";
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            CreateAksStep.AKS_ID,
            aksId,
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            "logAnalyticsWorkspaceId"));
    mockArmManagers();

    var exception = mockNonRetryableException();
    doThrow(exception).when(mockKubernetesClusterUpdate).apply();

    StepResult stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
  }

  @ParameterizedTest
  @MethodSource("workingParametersProvider")
  void doStepMissingWorkingParameterThrowsException(Map<String, Object> workingParameters) {
    setupFlightContext(mockFlightContext, Map.of(), workingParameters);
    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    // verify step does anything except returning success
    assertThat(
        testStep.undoStep(mockFlightContext).getStepStatus(),
        equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyNoInteractions(mockArmManagers);
  }

  private void mockArmManagers() {
    when(mockKubernetesClusterUpdate.withAddOnProfiles(addonProfileCaptor.capture()))
        .thenReturn(mockKubernetesClusterUpdate);
    when(mockKubernetesCluster.update()).thenReturn(mockKubernetesClusterUpdate);
    when(mockKubernetesClusters.getById(aksIdCaptor.capture())).thenReturn(mockKubernetesCluster);
    when(mockAzureResourceManager.kubernetesClusters()).thenReturn(mockKubernetesClusters);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
  }

  private ManagementException mockManagementException(int statusCode) {
    var azureException = mock(ManagementException.class);
    var httpResponse = mock(HttpResponse.class);
    when(httpResponse.getStatusCode()).thenReturn(statusCode);
    when(azureException.getResponse()).thenReturn(httpResponse);
    return azureException;
  }

  private ManagementException mockNonRetryableException() {
    return mockManagementException(HttpStatus.LOOP_DETECTED.value());
  }

  private ManagementException mockRetryableException() {
    return mockManagementException(HttpStatus.SERVICE_UNAVAILABLE.value());
  }

  private void verifyAddonProfile(Map<String, ManagedClusterAddonProfile> addonProfile) {
    ManagedClusterAddonProfile profile = addonProfile.get("omsagent");
    assertNotNull(profile);
    assertTrue(profile.enabled());
    assertTrue(profile.config().containsKey("useAADAuth"), "'useAADAuth' should be set.");
    assertTrue(
        profile.config().containsKey("logAnalyticsWorkspaceResourceID"),
        "'logAnalyticsWorkspaceResourceID' should be used.");
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
