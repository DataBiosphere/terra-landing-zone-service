package bio.terra.lz.futureservice.common.utils;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class RequestQueryParamUtils {
  private RequestQueryParamUtils() {}

  public static boolean isBillingProfileIdSanitized(UUID value, String originalRequestValue) {
    String[] queryParam = originalRequestValue.split("=");
    if (value != null || queryParam.length < 2) {
      // it seems the value of the parameter wasn't initially supplied,
      // hence the query parameter's value wasn't sanitized.
      return false;
    }
    return !StringUtils.isEmpty(queryParam[1]); // value is null here;
  }
}
