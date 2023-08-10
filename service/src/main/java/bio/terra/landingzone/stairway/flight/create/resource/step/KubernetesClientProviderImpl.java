package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.jetbrains.annotations.NotNull;

public class KubernetesClientProviderImpl implements KubernetesClientProvider {
  @NotNull
  public CoreV1Api createCoreApiClient(
      ArmManagers armManagers, String mrgName, String aksClusterName) {
    KubeConfig kubeConfig = loadKubeConfig(armManagers, mrgName, aksClusterName);
    var userToken = kubeConfig.getCredentials().get("token");

    ApiClient client =
        Config.fromToken(kubeConfig.getServer(), userToken)
            .setSslCaCert(
                new ByteArrayInputStream(
                    Base64.getDecoder()
                        .decode(
                            kubeConfig
                                .getCertificateAuthorityData()
                                .getBytes(StandardCharsets.UTF_8))));
    return new CoreV1Api(client);
  }

  @NotNull
  private KubeConfig loadKubeConfig(
      ArmManagers armManagers, String mrgName, String aksClusterName) {
    var rawKubeConfig =
        armManagers
            .azureResourceManager()
            .kubernetesClusters()
            .manager()
            .serviceClient()
            .getManagedClusters()
            .listClusterAdminCredentials(mrgName, aksClusterName)
            .kubeconfigs()
            .stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No kubeconfig found"));
    var kubeConfig =
        KubeConfig.loadKubeConfig(
            new InputStreamReader(
                new ByteArrayInputStream(rawKubeConfig.value()), StandardCharsets.UTF_8));
    return kubeConfig;
  }
}
