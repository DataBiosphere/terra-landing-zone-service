package bio.terra.landingzone.stairway.flight.exception.translation;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;

public class ExceptionFixtures {
  public static ManagementException createBatchAccountQuotaException() {
    return new ManagementException(
        BatchAccountQuotaExceedExceptionRule.POLLING_MESSAGE_MARKER,
        null,
        new ManagementError(
            "code", BatchAccountQuotaExceedExceptionRule.BATCH_QUOTA_MESSAGE_MARKER));
  }

  public static ManagementException createFlexiblePostgresRegionRestrictionException() {
    return new ManagementException(
        FlexiblePostgresRegionRestrictedOfferExceptionRule.LONG_RUNNING_OPERATION_MESSAGE_MARKER,
        null,
        new ManagementError(
            "code",
            FlexiblePostgresRegionRestrictedOfferExceptionRule
                .FLEXIBLE_POSTGRES_REGION_RESTRICTION_MESSAGE_MARKER));
  }

  public static ManagementException createNonMatchingManagementException() {
    return new ManagementException(
        "not matching message", null, new ManagementError("code", "non matching internal message"));
  }
}
