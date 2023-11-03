package bio.terra.landingzone.stairway.flight.exception;

import bio.terra.common.exception.BadRequestException;

public class MissingRequiredFieldsException extends BadRequestException {
  public MissingRequiredFieldsException(String message) {
    super(message);
  }
}
