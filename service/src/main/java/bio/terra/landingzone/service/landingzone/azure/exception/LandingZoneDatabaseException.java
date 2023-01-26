package bio.terra.landingzone.service.landingzone.azure.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class LandingZoneDatabaseException extends InternalServerErrorException {
  public LandingZoneDatabaseException(String message) {
    super(message);
  }
}
