package bio.terra.landingzone.common.k8s.configmap.reader;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ContainerLogV2ValidationTest {
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
    ContainerLogV2ConfigMapValidator.validate(configMap);
  }
}
