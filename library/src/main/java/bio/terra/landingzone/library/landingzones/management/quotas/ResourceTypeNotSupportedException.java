package bio.terra.landingzone.library.landingzones.management.quotas;

public class ResourceTypeNotSupportedException extends RuntimeException {
  public ResourceTypeNotSupportedException(String message) {
    super(message);
  }
}
