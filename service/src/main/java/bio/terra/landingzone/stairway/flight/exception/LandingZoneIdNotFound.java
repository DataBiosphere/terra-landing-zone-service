package bio.terra.landingzone.stairway.flight.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class LandingZoneIdNotFound extends InternalServerErrorException {
  public LandingZoneIdNotFound(String message) {
    super(message);
  }
}
