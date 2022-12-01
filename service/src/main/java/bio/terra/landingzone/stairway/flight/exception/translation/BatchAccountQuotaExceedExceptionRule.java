package bio.terra.landingzone.stairway.flight.exception.translation;

import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import com.azure.core.management.exception.ManagementException;
import java.util.Optional;

/**
 * Validates that exception is related to batch account quota issue and map it into new exception
 * with appropriate message
 */
class BatchAccountQuotaExceedExceptionRule implements ExceptionMatchingRule {
  public static final String POLLING_MESSAGE_MARKER = "Polling failed with status code";
  public static final String BATCH_QUOTA_MESSAGE_MARKER =
      "The regional Batch account quota for the specified subscription has been reached";

  @Override
  public Optional<LandingZoneCreateException> match(Exception exception) {
    if ((exception instanceof ManagementException)
        && (exception.getMessage().contains(POLLING_MESSAGE_MARKER))
        && ((ManagementException) exception)
            // value is a ManagementError which contains more appropriate information about the
            // quota issue
            .getValue()
            .getMessage()
            .contains(BATCH_QUOTA_MESSAGE_MARKER)) {
      return Optional.of(
          new LandingZoneCreateException(
              ((ManagementException) exception).getValue().getMessage()));
    }
    return Optional.empty();
  }
}
