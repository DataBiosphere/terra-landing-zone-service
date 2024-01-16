package bio.terra.landingzone.stairway.flight.create.resource.step;

import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneFederatedIdentityStep.k8sNamespace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.msi.MsiManager;
import com.azure.resourcemanager.msi.fluent.FederatedIdentityCredentialsClient;
import com.azure.resourcemanager.msi.fluent.ManagedServiceIdentityClient;
import com.azure.resourcemanager.msi.fluent.models.FederatedIdentityCredentialInner;
import com.azure.resourcemanager.msi.models.Identities;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CreateLandingZoneFederatedIdentityStepTest extends BaseStepTest {
  private static final String oidcIssuer = UUID.randomUUID().toString();
  private CreateLandingZoneFederatedIdentityStep testStep;
  @Mock private Identities mockIdentities;
  @Mock private KubernetesClientProvider mockKubernetesClientProvider;
  @Mock MsiManager mockManager;
  @Mock ManagedServiceIdentityClient mockServiceClient;
  @Mock FederatedIdentityCredentialsClient mockFederatedIdentityCredentials;
  @Captor private ArgumentCaptor<FederatedIdentityCredentialInner> federatedIdentityCaptor;
  @Mock private CoreV1Api mockKubernetesClient;
  @Captor private ArgumentCaptor<V1ServiceAccount> serviceAccountCaptor;

  @BeforeEach
  void setup() {
    testStep = new CreateLandingZoneFederatedIdentityStep(mockKubernetesClientProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException, ApiException {
    final String uamiName = UUID.randomUUID().toString();

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    final String uamiClientId = UUID.randomUUID().toString();
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(uamiName).build(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_CLIENT_ID,
            uamiClientId,
            CreateAksStep.AKS_OIDC_ISSUER_URL,
            oidcIssuer,
            CreateAksStep.AKS_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(UUID.randomUUID().toString()).build()));
    setupArmManagersForDoStep(uamiName, mrg);

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(federatedIdentityCaptor.getValue().issuer(), equalTo(oidcIssuer));
    assertThat(
        federatedIdentityCaptor.getValue().audiences(),
        equalTo(List.of("api://AzureADTokenExchange")));
    assertThat(
        federatedIdentityCaptor.getValue().subject(),
        equalTo(String.format("system:serviceaccount:%s:%s", k8sNamespace, uamiName)));

    assertThat(
        serviceAccountCaptor.getValue().getMetadata().getAnnotations(),
        equalTo(Map.of("azure.workload.identity/client-id", uamiClientId)));
    assertThat(serviceAccountCaptor.getValue().getMetadata().getName(), equalTo(uamiName));
    assertThat(serviceAccountCaptor.getValue().getMetadata().getNamespace(), equalTo(k8sNamespace));

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
  }

  @Test
  void undoStepSuccess() throws InterruptedException, ApiException {
    final String uamiName = UUID.randomUUID().toString();
    var resourceId = "resourceId";
    final TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();

    var workingMap =
        Map.of(
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_ID,
            resourceId,
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(uamiName).build(),
            CreateAksStep.AKS_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(UUID.randomUUID().toString()).build());

    setupFlightContext(mockFlightContext, Map.of(), workingMap);

    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.identities()).thenReturn(mockIdentities);
    when(mockIdentities.manager()).thenReturn(mockManager);
    when(mockManager.serviceClient()).thenReturn(mockServiceClient);
    when(mockServiceClient.getFederatedIdentityCredentials())
        .thenReturn(mockFederatedIdentityCredentials);

    when(mockKubernetesClientProvider.createCoreApiClient(any(), any(), any()))
        .thenReturn(mockKubernetesClient);

    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockKubernetesClient)
        .deleteNamespacedServiceAccount(
            eq(uamiName), eq(k8sNamespace), any(), any(), any(), any(), any(), any());
    verify(mockFederatedIdentityCredentials).delete(mrg.name(), uamiName, uamiName);
  }

  private void setupArmManagersForDoStep(String uamiName, TargetManagedResourceGroup mrg)
      throws ApiException {
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.identities()).thenReturn(mockIdentities);
    when(mockIdentities.manager()).thenReturn(mockManager);
    when(mockManager.serviceClient()).thenReturn(mockServiceClient);
    when(mockServiceClient.getFederatedIdentityCredentials())
        .thenReturn(mockFederatedIdentityCredentials);
    when(mockFederatedIdentityCredentials.createOrUpdate(
            eq(mrg.name()), eq(uamiName), eq(uamiName), federatedIdentityCaptor.capture()))
        .thenReturn(null);

    when(mockKubernetesClientProvider.createCoreApiClient(any(), any(), any()))
        .thenReturn(mockKubernetesClient);
    when(mockKubernetesClient.createNamespacedServiceAccount(
            eq(k8sNamespace), serviceAccountCaptor.capture(), any(), any(), any(), any()))
        .thenReturn(null);
  }
}
