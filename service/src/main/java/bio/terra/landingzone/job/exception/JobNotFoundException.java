package bio.terra.landingzone.job.exception;

import bio.terra.common.exception.ErrorReportException;

public class JobNotFoundException extends ErrorReportException {
  public JobNotFoundException(String message) {
    super(message);
  }

  public JobNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public JobNotFoundException(Throwable cause) {
    super(cause);
  }
}
