package bio.terra.landingzone.job.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class ExceptionSerializerException extends InternalServerErrorException {
  public ExceptionSerializerException(String message) {
    super(message);
  }

  public ExceptionSerializerException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExceptionSerializerException(Throwable cause) {
    super(cause);
  }
}
