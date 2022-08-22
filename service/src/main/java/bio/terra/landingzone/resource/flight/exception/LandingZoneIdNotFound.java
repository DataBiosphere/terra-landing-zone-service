package bio.terra.landingzone.resource.flight.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class LandingZoneIdNotFound extends InternalServerErrorException {
  public LandingZoneIdNotFound(String message) {
    super(message);
  }
}
