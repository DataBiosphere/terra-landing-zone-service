package bio.terra.landingzone.resource.flight.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class AzureLandingZoneIdNotFound extends InternalServerErrorException {
  public AzureLandingZoneIdNotFound(String message) {
    super(message);
  }
}
