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
import com.azure.resourcemanager.msi.fluent.models.FederatedIdentityCredentialInner;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CreateLandingZoneFederatedIdentityStep implements Step {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateLandingZoneFederatedIdentityStep.class);
  public static final String k8sNamespace = "default";
  private final ArmManagers armManagers;
  private final KubernetesClientProvider kubernetesClientProvider;

  public CreateLandingZoneFederatedIdentityStep(
      ArmManagers armManagers, KubernetesClientProvider kubernetesClientProvider) {
    this.armManagers = armManagers;
    this.kubernetesClientProvider = kubernetesClientProvider;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    var uami =
        FlightUtils.getRequired(
            context.getWorkingMap(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            LandingZoneResource.class);
    var uamiName = uami.resourceName().orElseThrow();
    var uamiClientId =
        FlightUtils.getRequired(
            context.getWorkingMap(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_CLIENT_ID,
            String.class);
    var oidcIssuer =
        FlightUtils.getRequired(
            context.getWorkingMap(), CreateAksStep.AKS_OIDC_ISSUER_URL, String.class);
    var aksResource =
        FlightUtils.getRequired(
            context.getWorkingMap(), CreateAksStep.AKS_RESOURCE_KEY, LandingZoneResource.class);
    var mrgName = getMRGName(context);

    createFederatedCredentials(context, k8sNamespace, uamiName, oidcIssuer);

    try {
      createK8sServiceAccount(
          k8sNamespace, uamiName, uamiClientId, aksResource.resourceName().orElseThrow(), mrgName);
    } catch (ApiException e) {
      logger.info("Failed to create k8s service account", e);
      if (e.getCode() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
      } else {
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
      }
    }

    return StepResult.getStepResultSuccess();
  }

  private void createK8sServiceAccount(
      String k8sNamespace,
      String uamiName,
      String uamiClientId,
      String aksClusterName,
      String mrgName)
      throws ApiException {
    CoreV1Api api =
        kubernetesClientProvider.createCoreApiClient(armManagers, mrgName, aksClusterName);

    var aksServiceAccount =
        new V1ServiceAccount()
            .metadata(
                new V1ObjectMeta()
                    .annotations(Map.of("azure.workload.identity/client-id", uamiClientId))
                    .name(uamiName) // name of the service account is the same as the user-assigned
                    .namespace(k8sNamespace));

    api.createNamespacedServiceAccount(k8sNamespace, aksServiceAccount, null, null, null, null);
  }

  private void deleteK8sServiceAccount(
      String k8sNamespace, String uamiName, String aksClusterName, String mrgName)
      throws ApiException {
    CoreV1Api api =
        kubernetesClientProvider.createCoreApiClient(armManagers, mrgName, aksClusterName);

    api.deleteNamespacedServiceAccount(
        uamiName, k8sNamespace, null, null, null, null, null, new V1DeleteOptions());
  }

  private void createFederatedCredentials(
      FlightContext context, String k8sNamespace, String uamiName, String oidcIssuer) {
    armManagers
        .azureResourceManager()
        .identities()
        .manager()
        .serviceClient()
        .getFederatedIdentityCredentials()
        .createOrUpdate(
            getMRGName(context),
            uamiName,
            uamiName, // name of federated identity is the same as the user-assigned identity
            new FederatedIdentityCredentialInner()
                .withIssuer(oidcIssuer)
                .withAudiences(List.of("api://AzureADTokenExchange"))
                .withSubject(String.format("system:serviceaccount:%s:%s", k8sNamespace, uamiName)));
  }

  private void deleteFederatedCredentials(FlightContext context, String uamiName) {
    armManagers
        .azureResourceManager()
        .identities()
        .manager()
        .serviceClient()
        .getFederatedIdentityCredentials()
        .delete(getMRGName(context), uamiName, uamiName);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    var aksResource =
        context.getWorkingMap().get(CreateAksStep.AKS_RESOURCE_KEY, LandingZoneResource.class);
    var uami =
        context
            .getWorkingMap()
            .get(
                CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
                LandingZoneResource.class);
    if (uami == null || aksResource == null) {
      return StepResult.getStepResultSuccess();
    }

    var uamiName = uami.resourceName().orElseThrow();
    var mrgName = getMRGName(context);

    try {
      deleteK8sServiceAccount(
          k8sNamespace, uamiName, aksResource.resourceName().orElseThrow(), mrgName);
    } catch (ApiException e) {
      logger.info("Failed to delete k8s service account", e);
    }
    deleteFederatedCredentials(context, uamiName);
    return StepResult.getStepResultSuccess();
  }

  protected String getMRGName(FlightContext context) {
    return FlightUtils.getRequired(
            context.getWorkingMap(),
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            TargetManagedResourceGroup.class)
        .name();
  }
}
