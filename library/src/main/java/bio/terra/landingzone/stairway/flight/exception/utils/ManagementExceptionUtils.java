package bio.terra.landingzone.stairway.flight.exception.utils;

import com.azure.core.management.exception.ManagementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManagementExceptionUtils {
  private ManagementExceptionUtils() {}

  public static String buildErrorInfo(ManagementException e) {
    StringBuilder errorDetails = new StringBuilder();
    errorDetails.append("ErrorMessage: %s; ".formatted(e.getMessage()));
    if (e.getValue() != null) {
      errorDetails.append(
          "ErrorCode: %s; AdditionalMessage: %s;"
              .formatted(
                  Optional.of(e.getValue().getCode()).orElse("n/a"),
                  Optional.of(e.getValue().getMessage()).orElse("n/a")));
      if (e.getValue().getDetails() != null) {
        errorDetails.append(
            "Details: %s;"
                .formatted(
                    e.getValue().getDetails().stream()
                        .map(
                            me ->
                                "[code: %s, message: %s]".formatted(me.getCode(), me.getMessage()))
                        .collect(Collectors.joining(","))));
      }
    }
    return errorDetails.toString();
  }
}
