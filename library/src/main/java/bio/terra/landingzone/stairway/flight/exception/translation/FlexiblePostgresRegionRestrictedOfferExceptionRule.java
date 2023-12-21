package bio.terra.landingzone.stairway.flight.exception.translation;

import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import com.azure.core.management.exception.ManagementException;
import java.util.Optional;

/**
 * Validates that exception is related to Flexible Postgresql region restriction issue and maps it
 * into new exception with appropriate message
 */
public class FlexiblePostgresRegionRestrictedOfferExceptionRule implements ExceptionMatchingRule {
  public static final String LONG_RUNNING_OPERATION_MESSAGE_MARKER =
      "Long running operation is Failed or Cancelled";
  public static final String FLEXIBLE_POSTGRES_REGION_RESTRICTION_MESSAGE_MARKER =
      "Subscriptions are restricted from provisioning in this region";

  @Override
  public Optional<LandingZoneCreateException> match(Exception exception) {
    if ((exception instanceof ManagementException managementException)
        && (managementException.getMessage().contains(LONG_RUNNING_OPERATION_MESSAGE_MARKER))
        && managementException
            .getValue()
            .getMessage()
            .contains(FLEXIBLE_POSTGRES_REGION_RESTRICTION_MESSAGE_MARKER)) {
      return Optional.of(
          new LandingZoneCreateException(
              managementException.getValue().getMessage(), managementException));
    }
    return Optional.empty();
  }
}
