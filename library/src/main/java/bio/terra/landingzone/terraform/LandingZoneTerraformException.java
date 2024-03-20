package bio.terra.landingzone.terraform;

public class LandingZoneTerraformException extends RuntimeException {
  public LandingZoneTerraformException(String message) {
    super(message);
  }

  public LandingZoneTerraformException(String message, Throwable cause) {
    super(message, cause);
  }
}
