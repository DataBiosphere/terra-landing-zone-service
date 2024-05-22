package bio.terra.lz.futureservice.common.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestQueryParamUtilsTest {
  private final String billingProfileIdQueryParam = "billingProfileId=%s";
  private final String billingProfileIdAndOtherQueryParam = "billingProfileId=%s&param1=value1";
  private final UUID billingProfileId = UUID.randomUUID();

  @Test
  void testWhenOriginalValueProvided() {
    assertFalse(
        RequestQueryParamUtils.isBillingProfileIdSanitized(
            billingProfileId, billingProfileIdQueryParam.formatted(billingProfileId)));
  }

  @Test
  void testWhenOriginalValueNotProvided() {
    // original value is not provided, but the request contains corresponding query parameter.
    assertTrue(
        RequestQueryParamUtils.isBillingProfileIdSanitized(
            null, billingProfileIdQueryParam.formatted("valueToBeSanitized")));
  }

  @Test
  void testWhenOriginalValueNotProvided_AndMultipleQueryParamsProvided() {
    // this is unlikely situation, but we might have some additional parameters provided
    // (intentionally or not) together with billingProfileId;
    // currently we support billingProfileId only
    assertTrue(
        RequestQueryParamUtils.isBillingProfileIdSanitized(
            null, billingProfileIdAndOtherQueryParam.formatted("valueToBeSanitized")));
  }

  @Test
  void testWhenOriginalValueNotProvided_MultipleQueryParamsProvidedWithoutBillingProfileId() {
    // this is unlikely situation, but we might have some parameters provided
    // (intentionally or not) without billingProfileId
    assertFalse(
        RequestQueryParamUtils.isBillingProfileIdSanitized(null, "param1=value1&param2=value2"));
  }

  @Test
  void testWhenRequestDoesntHaveQueryParam() {
    assertFalse(RequestQueryParamUtils.isBillingProfileIdSanitized(null, null));
  }
}
