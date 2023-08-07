package bio.terra.landingzone.common.utils;

import org.springframework.http.HttpStatus;

public class HttpResponseUtils {
  private HttpResponseUtils() {}

  public static boolean isRetryable(int httpStatusCode) {
    return httpStatusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()
        || httpStatusCode == HttpStatus.BAD_GATEWAY.value()
        || httpStatusCode == HttpStatus.SERVICE_UNAVAILABLE.value()
        || httpStatusCode == HttpStatus.GATEWAY_TIMEOUT.value();
  }
}
