package bio.terra.landingzone.common.k8s.configmap.reader;

public class AksConfigMapReaderException extends Exception {
  public AksConfigMapReaderException(String message) {
    super(message);
  }

  public AksConfigMapReaderException(String message, Throwable cause) {
    super(message, cause);
  }
}
