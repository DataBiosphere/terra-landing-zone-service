package bio.terra.lz.futureservice.app.service.exception;

import bio.terra.common.exception.BadRequestException;

public class LandingZoneInvalidInputException extends BadRequestException {
  public LandingZoneInvalidInputException(String message) {
    super(message);
  }
}
