package bio.terra.landingzone.stairway.flight.exception;

public class LandingZoneCreateException extends LandingZoneProcessingException {
  public LandingZoneCreateException(String message) {
    super(message);
  }

  public LandingZoneCreateException(String message, Throwable cause) {
    super(message, cause);
  }
}
