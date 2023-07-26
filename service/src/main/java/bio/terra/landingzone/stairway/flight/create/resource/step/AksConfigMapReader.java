package bio.terra.landingzone.stairway.flight.create.resource.step;

import io.kubernetes.client.openapi.models.V1ConfigMap;

public interface AksConfigMapReader {
  V1ConfigMap read() throws AksConfigMapReaderException;
}
