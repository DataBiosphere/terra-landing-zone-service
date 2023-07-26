package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
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

/**
 * This class implements functionality to apply specific ConfigMap to AKS to enable ContainerLogV2
 * schema.
 *
 * <p>https://learn.microsoft.com/en-us/azure/azure-monitor/containers/container-insights-logging-v2#enable-the-containerlogv2-schema
 */
public class EnableAksContainerLogV2Step implements Step {
  public static final String CONFIG_MAP_PATH = "landingzone/aks/configmap/ContainerLogV2.yaml";

  private static final Logger logger = LoggerFactory.getLogger(EnableAksContainerLogV2Step.class);

  private final ArmManagers armManagers;
  private final KubernetesClientProvider kubernetesClientProvider;
  private final AksConfigMapReader aksConfigMapReader;

  public EnableAksContainerLogV2Step(
      ArmManagers armManagers,
      KubernetesClientProvider kubernetesClientProvider,
      AksConfigMapReader aksConfigMapReader) {
    this.armManagers = armManagers;
    this.kubernetesClientProvider = kubernetesClientProvider;
    this.aksConfigMapReader = aksConfigMapReader;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightUtils.validateRequiredEntries(
        context.getInputParameters(), CreateAksStep.AKS_RESOURCE_KEY);
    var aks =
        FlightUtils.getRequired(
            context.getInputParameters(),
            CreateAksStep.AKS_RESOURCE_KEY,
            LandingZoneResource.class);
    FlightUtils.validateRequiredEntries(
        context.getWorkingMap(), GetManagedResourceGroupInfo.TARGET_MRG_KEY);
    var mrg =
        FlightUtils.getRequired(
            context.getWorkingMap(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            TargetManagedResourceGroup.class);
    try {
      var containerLogV2ConfigMap = aksConfigMapReader.read();
      createContainerLogV2ConfigMap(
          containerLogV2ConfigMap, mrg.name(), aks.resourceName().get(), "default");
    } catch (ApiException e) {
      logger.info(
          String.format(
              "Failed to apply ContainerLogV2 configmap to AKS cluster. AKS id: '%s'",
              aks.resourceId()),
          e);
      if (isK8sApiRetryableError(e.getCode())) {
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
      } else {
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
      }
    } catch (AksConfigMapReaderException e) {
      logger.info("Failed to initialize k8s config map for ContainerLogV2", e);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }

    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // no need to remove configmap. it will be deleted together with k8s.
    return StepResult.getStepResultSuccess();
  }

  private void createContainerLogV2ConfigMap(
      V1ConfigMap containerLogV2ConfigMap,
      String managedResourceGroupName,
      String aksResourceName,
      String aksNamespace)
      throws ApiException {
    CoreV1Api k8sApiClient =
        kubernetesClientProvider.createCoreApiClient(
            armManagers, managedResourceGroupName, aksResourceName);
    k8sApiClient.createNamespacedConfigMap(
        aksNamespace, containerLogV2ConfigMap, null, null, null, null);
  }

  private boolean isK8sApiRetryableError(int httpStatusCode) {
    return httpStatusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()
        || httpStatusCode == HttpStatus.BAD_GATEWAY.value()
        || httpStatusCode == HttpStatus.SERVICE_UNAVAILABLE.value()
        || httpStatusCode == HttpStatus.GATEWAY_TIMEOUT.value();
  }
}
