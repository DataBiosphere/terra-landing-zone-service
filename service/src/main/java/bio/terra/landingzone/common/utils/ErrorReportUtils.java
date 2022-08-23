package bio.terra.landingzone.common.utils;

import bio.terra.common.exception.ErrorReportException;
import bio.terra.landingzone.job.model.ErrorReport;
import org.springframework.http.HttpStatus;

public class ErrorReportUtils {
  public static ErrorReport buildApiErrorReport(Exception exception) {
    if (exception instanceof ErrorReportException) {
      ErrorReportException errorReport = (ErrorReportException) exception;
      return new ErrorReport()
          .message(errorReport.getMessage())
          .statusCode(errorReport.getStatusCode().value())
          .causes(errorReport.getCauses());
    } else {
      return new ErrorReport()
          .message(exception.getMessage())
          .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
          .causes(null);
    }
  }
}
