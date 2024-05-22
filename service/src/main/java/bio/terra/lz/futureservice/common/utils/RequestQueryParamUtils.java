package bio.terra.lz.futureservice.common.utils;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class RequestQueryParamUtils {
  private static final String BILLING_PROFILE_ID_PARAM_NAME = "billingProfileId";

  private RequestQueryParamUtils() {}

  public static boolean isBillingProfileIdSanitized(
      UUID billingProfileId, String requestQueryParamPairs) {
    if (billingProfileId != null) return false;
    if (requestQueryParamPairs == null) return false;

    // unlikely, but we might have more than 1 parameter provided;
    // billingProfileId=test&param1=value1
    String[] allQueryParamsPairs = requestQueryParamPairs.split("&");
    Optional<String> billingProfileIdQueryParamPair =
        Arrays.stream(allQueryParamsPairs)
            .filter(pairs -> pairs.contains(BILLING_PROFILE_ID_PARAM_NAME))
            .findFirst();

    if (billingProfileIdQueryParamPair.isEmpty()) return false;

    String[] billingProfileIdKeyValuePair = billingProfileIdQueryParamPair.get().split("=");
    String billingProfileIdQueryValue =
        billingProfileIdKeyValuePair.length == 2 ? billingProfileIdKeyValuePair[1] : null;
    // if we are here billingProfileId is null
    return !StringUtils.isEmpty(billingProfileIdQueryValue);
  }
}
