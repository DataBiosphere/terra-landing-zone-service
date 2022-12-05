package bio.terra.landingzone.stairway.flight.exception;

public class LandingZoneProcessingException extends RuntimeException {
  public LandingZoneProcessingException(String message) {
    super(message);
  }

  public LandingZoneProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
