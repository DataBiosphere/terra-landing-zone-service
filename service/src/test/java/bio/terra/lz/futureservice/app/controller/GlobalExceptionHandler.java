package bio.terra.lz.futureservice.app.controller;

import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import bio.terra.lz.futureservice.generated.model.ApiErrorReport;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler<ApiErrorReport> {
  @Override
  public ApiErrorReport generateErrorReport(
      Throwable ex, HttpStatus statusCode, List<String> causes) {
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
    var errorReport =
        new ApiErrorReport()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .message("Invalid request " + ex.getClass().getSimpleName());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReport);
  }
}
