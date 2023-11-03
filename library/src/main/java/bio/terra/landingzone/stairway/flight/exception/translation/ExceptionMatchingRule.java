package bio.terra.landingzone.stairway.flight.exception.translation;

import bio.terra.landingzone.stairway.flight.exception.LandingZoneProcessingException;
import java.util.Optional;

/** Base interface for exception matchers */
interface ExceptionMatchingRule {
  /**
   * Maps original exception into new one if it matches specific rule
   *
   * @param exception exception to match
   * @return Matched exception or original one
   */
  Optional<? extends LandingZoneProcessingException> match(Exception exception);
}
