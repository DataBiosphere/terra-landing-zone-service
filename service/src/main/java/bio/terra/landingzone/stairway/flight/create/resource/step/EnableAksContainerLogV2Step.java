package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class EnableAksContainerLogV2Step implements Step {
  private static final Logger logger = LoggerFactory.getLogger(EnableAksContainerLogV2Step.class);

  private final ArmManagers armManagers;
  private final KubernetesClientProvider kubernetesClientProvider;

  public EnableAksContainerLogV2Step(
      ArmManagers armManagers, KubernetesClientProvider kubernetesClientProvider) {
    this.armManagers = armManagers;
    this.kubernetesClientProvider = kubernetesClientProvider;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var aks =
        context.getInputParameters().get(CreateAksStep.AKS_RESOURCE_KEY, LandingZoneResource.class);
    var mrg =
        context
            .getWorkingMap()
            .get(GetManagedResourceGroupInfo.TARGET_MRG_KEY, TargetManagedResourceGroup.class);

    try {
      //TODO: is it possible to pass comman separated list of namespaces?
      createContainerLogV2ConfigMap(mrg.name(), aks.resourceName().get(), "default");
    } catch (ApiException e) {
      logger.info("Failed to create k8s config map for ContainerLogV2", e);
      if (isK8sApiRetryableError(e.getCode())) {
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
      } else {
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
      }
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    return null;
  }

  private boolean isK8sApiRetryableError(int httpStatusCode) {
    return httpStatusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()
        || httpStatusCode == HttpStatus.BAD_GATEWAY.value()
        || httpStatusCode == HttpStatus.SERVICE_UNAVAILABLE.value()
        || httpStatusCode == HttpStatus.GATEWAY_TIMEOUT.value();
  }

  private void createContainerLogV2ConfigMap(
      String managedResourceGroupName, String aksResourceName, String aksNamespace)
      throws ApiException {
    CoreV1Api k8sApiClient =
        kubernetesClientProvider.createCoreApiClient(
            armManagers, managedResourceGroupName, aksResourceName);
    var containerLogV2ConfigMap = buildContainerLogV2ConfigMap();

    k8sApiClient.createNamespacedConfigMap(
        aksNamespace, containerLogV2ConfigMap, null, null, null, null);
  }

  private V1ConfigMap buildContainerLogV2ConfigMap() {
    V1ConfigMap containerLogV2ConfigMap = new V1ConfigMap();
      containerLogV2ConfigMap.
    // containerLogV2ConfigMap.
    return containerLogV2ConfigMap;
  }
}
