package bio.terra.landingzone.common.exception;

import bio.terra.common.exception.InternalServerErrorException;

/** When you can't get there from here, but somehow end up there */
public class MissingRequiredFieldsException extends InternalServerErrorException {
  public MissingRequiredFieldsException(String message) {
    super(message);
  }
}
