package bio.terra.landingzone.common.k8s.configmap.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kubernetes.client.openapi.models.V1ConfigMap;

/** Validate internals of K8s ConfigMap specific to ContainerLogV2 configuration */
public class ContainerLogV2ConfigMapValidator {
  private ContainerLogV2ConfigMapValidator() {}

  public static void validate(V1ConfigMap configMap) {
    assertNotNull(configMap);
    assertThat(configMap.getApiVersion(), equalTo("v1"));
    assertThat(configMap.getKind(), equalTo("ConfigMap"));
    assertNotNull(configMap.getMetadata());
    assertThat(configMap.getMetadata().getNamespace(), equalTo("kube-system"));
    assertNotNull(configMap.getData());
    assertNotNull(configMap.getData().get("log-data-collection-settings"));
    assertTrue(
        configMap
            .getData()
            .get("log-data-collection-settings")
            .contains("containerlog_schema_version = \"v2\""));
  }
}
