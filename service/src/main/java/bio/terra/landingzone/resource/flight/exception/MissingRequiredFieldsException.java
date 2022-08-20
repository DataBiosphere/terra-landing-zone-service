package bio.terra.landingzone.resource.flight.exception;

import bio.terra.common.exception.BadRequestException;

public class MissingRequiredFieldsException extends BadRequestException {
  public MissingRequiredFieldsException(String message) {
    super(message);
  }
}
