package bio.terra.landingzone.common.k8s.configmap.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ContainerLogV2ValidationTest {
  private static final String LIVE_FILE_PATH_PREFIX = "landingzone/aks/configmap/";

  /**
   * This test validates that live ContainerLogV2.yaml configuration is not corrupted
   *
   * @throws AksConfigMapReaderException
   */
  @Test
  void readSuccess() throws AksConfigMapReaderException {
    AksConfigMapReader configMapReader =
        new AksConfigMapFileReaderImpl(LIVE_FILE_PATH_PREFIX + "ContainerLogV2.yaml");

    V1ConfigMap configMap = configMapReader.read();

    assertNotNull(configMap);
    assertThat(configMap.getApiVersion(), equalTo("v1"));
    assertThat(configMap.getKind(), equalTo("ConfigMap"));
    assertNotNull(configMap.getMetadata());
    assertThat(configMap.getMetadata().getNamespace(), equalTo("kube-system"));
  }
}
