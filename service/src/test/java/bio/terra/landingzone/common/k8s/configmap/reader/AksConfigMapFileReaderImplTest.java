package bio.terra.landingzone.common.k8s.configmap.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AksConfigMapFileReaderImplTest {
  private static final String TEST_FILE_PATH_PREFIX = "test/landingzone/aks/configmap/";

  AksConfigMapReader configMapReader;

  @Test
  void readSuccess() throws AksConfigMapReaderException {
    configMapReader = new AksConfigMapFileReaderImpl(TEST_FILE_PATH_PREFIX + "ContainerLogV2.yaml");

    V1ConfigMap configMap = configMapReader.read();

    assertNotNull(configMap);
    assertThat(configMap.getApiVersion(), equalTo("v1"));
    assertThat(configMap.getKind(), equalTo("ConfigMap"));
    assertNotNull(configMap.getMetadata());
    assertThat(configMap.getMetadata().getNamespace(), equalTo("kube-system"));
  }

  @Test
  void readFileWhichDoesntExistFailure() {
    configMapReader =
        new AksConfigMapFileReaderImpl(TEST_FILE_PATH_PREFIX + "FileDoesntExist.yaml");
    var ex = assertThrows(AksConfigMapReaderException.class, () -> configMapReader.read());
    assertTrue(ex.getMessage().contains("not found or access is denied"));
  }

  @Test
  void readFileWhichIsNotConfigMapFailure() {
    configMapReader =
        new AksConfigMapFileReaderImpl(TEST_FILE_PATH_PREFIX + "Deployment_NotConfigMap.yaml");
    var ex = assertThrows(AksConfigMapReaderException.class, () -> configMapReader.read());
    assertTrue(ex.getMessage().contains("doesn't contain ConfigMap definition"));
  }
}
