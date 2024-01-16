package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.common.k8s.configmap.reader.AksConfigMapReader;
import bio.terra.landingzone.common.k8s.configmap.reader.AksConfigMapReaderException;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.StepStatus;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
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
class EnableAksContainerLogV2StepTest extends BaseStepTest {
  @Mock private KubernetesClientProvider mockKubernetesClientProvider;
  @Mock private AksConfigMapReader mockAksConfigMapReader;
  @Mock private CoreV1Api mockCoreV1Api;

  @Captor private ArgumentCaptor<String> aksNameCaptor;
  @Captor private ArgumentCaptor<String> mrgNameCaptor;
  @Captor private ArgumentCaptor<String> aksNamespaceCaptor;

  EnableAksContainerLogV2Step testStep;

  @BeforeEach
  void setup() {
    testStep =
        new EnableAksContainerLogV2Step(mockKubernetesClientProvider, mockAksConfigMapReader);
  }

  @Test
  void doStepSuccess() throws InterruptedException, AksConfigMapReaderException, ApiException {
    final String aksClusterName = "aksName";
    final TargetManagedResourceGroup targetMrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            CreateAksStep.AKS_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(aksClusterName).build(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            targetMrg));

    var v1ConfigMap = mock(V1ConfigMap.class);
    when(mockAksConfigMapReader.read()).thenReturn(v1ConfigMap);
    when(mockKubernetesClientProvider.createCoreApiClient(
            any(), mrgNameCaptor.capture(), aksNameCaptor.capture()))
        .thenReturn(mockCoreV1Api);
    when(mockCoreV1Api.createNamespacedConfigMap(
            aksNamespaceCaptor.capture(), any(), any(), any(), any(), any()))
        .thenReturn(null);

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockKubernetesClientProvider, times(1)).createCoreApiClient(any(), any(), any());
    assertThat(mrgNameCaptor.getValue(), equalTo(targetMrg.name()));
    assertThat(aksNameCaptor.getValue(), equalTo(aksClusterName));
    verify(mockAksConfigMapReader, times(1)).read();
    verify(mockCoreV1Api, times(1))
        .createNamespacedConfigMap(anyString(), any(), eq(null), eq(null), eq(null), eq(null));
    assertThat(aksNamespaceCaptor.getValue(), equalTo("kube-system"));
  }

  @ParameterizedTest
  @MethodSource("workingParametersProvider")
  void doStepMissingWorkingParameterThrowsException(Map<String, Object> workingParameters) {
    setupFlightContext(mockFlightContext, Map.of(), workingParameters);
    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @Test
  void doStepFailedK8sApiThrowsException()
      throws InterruptedException, AksConfigMapReaderException, ApiException {
    final String aksClusterName = "aksName";
    final TargetManagedResourceGroup targetMrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            CreateAksStep.AKS_RESOURCE_KEY,
            LandingZoneResource.builder().resourceId("aksId").resourceName(aksClusterName).build(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            targetMrg));

    var v1ConfigMap = mock(V1ConfigMap.class);
    when(mockAksConfigMapReader.read()).thenReturn(v1ConfigMap);
    when(mockKubernetesClientProvider.createCoreApiClient(
            any(), mrgNameCaptor.capture(), aksNameCaptor.capture()))
        .thenReturn(mockCoreV1Api);

    var apiNonRetryableException = mock(ApiException.class);
    when(apiNonRetryableException.getCode()).thenReturn(HttpStatus.LOOP_DETECTED.value());
    doThrow(apiNonRetryableException)
        .when(mockCoreV1Api)
        .createNamespacedConfigMap(any(), any(), any(), any(), any(), any());

    var stepResult = testStep.doStep(mockFlightContext);
    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
  }

  @Test
  void doStepFailedConfigMapReaderThrowsException()
      throws AksConfigMapReaderException, InterruptedException {
    final String aksClusterName = "aksName";
    final TargetManagedResourceGroup targetMrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            CreateAksStep.AKS_RESOURCE_KEY,
            LandingZoneResource.builder().resourceId("aksId").resourceName(aksClusterName).build(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            targetMrg));

    doThrow(AksConfigMapReaderException.class).when(mockAksConfigMapReader).read();

    var stepResult = testStep.doStep(mockFlightContext);
    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        Arguments.of(
            Map.of(
                CreateAksStep.AKS_RESOURCE_KEY,
                LandingZoneResource.builder().resourceName("aksClusterName").build())),
        Arguments.of(
            Map.of(
                GetManagedResourceGroupInfo.TARGET_MRG_KEY,
                ResourceStepFixture.createDefaultMrg())));
  }
}
