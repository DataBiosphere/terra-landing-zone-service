package bio.terra.lz.futureservice.app.controller;

import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import bio.terra.common.exception.NotFoundException;
import bio.terra.lz.futureservice.generated.model.ApiErrorReport;
import io.sentry.Sentry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler<ApiErrorReport> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Override
  public ApiErrorReport generateErrorReport(
      Throwable ex, HttpStatus statusCode, List<String> causes) {
    if (statusCode.is5xxServerError()) {
      Sentry.captureException(ex);
    }
    return new ApiErrorReport()
        .message(ex.getMessage())
        .statusCode(statusCode.value())
        .causes(causes);
  }

  @Override
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiErrorReport> validationExceptionHandler(Exception ex) {
    LOGGER.error("ERROR", ex);
    var errorReport =
        new ApiErrorReport()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .message("Invalid request " + ex.getClass().getSimpleName());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReport);
  }

  @ExceptionHandler({NoResourceFoundException.class, NotFoundException.class})
  public ResponseEntity<ApiErrorReport> noResourceFoundHandler(Exception e) {
    var report = new ApiErrorReport().message("Not found").statusCode(HttpStatus.NOT_FOUND.value());
    return new ResponseEntity<>(report, HttpStatus.NOT_FOUND);
  }
}
