package bio.terra.landingzone.common.k8s.configmap.reader;

import io.kubernetes.client.openapi.models.V1ConfigMap;

public interface AksConfigMapReader {
  V1ConfigMap read() throws AksConfigMapReaderException;
}
