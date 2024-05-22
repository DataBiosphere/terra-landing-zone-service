package bio.terra.lz.futureservice.common.utils;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class RequestQueryParamUtils {
  private RequestQueryParamUtils() {}

  public static boolean isBillingProfileIdSanitized(
      UUID billingProfileId, String queryParamKeyPair) {
    if (queryParamKeyPair == null) return false;
    String[] billingProfileIdQueryParam = queryParamKeyPair.split("=");
    String billingProfileIdQueryValue =
        billingProfileIdQueryParam.length == 2 ? billingProfileIdQueryParam[1] : null;
    return (billingProfileId == null) && !StringUtils.isEmpty(billingProfileIdQueryValue);
  }
}
