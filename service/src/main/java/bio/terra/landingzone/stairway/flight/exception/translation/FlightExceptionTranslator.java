package bio.terra.landingzone.stairway.flight.exception.translation;

import java.util.ArrayList;

/**
 * Translates original landing zone flight exception into new one.
 *
 * <p>Original exception message might not reflect the real issue. In this case we can try to get
 * more appropriate information from the internals of original exception. Translation happens based
 * on set of predefined mapping rules. Current implementation contains rule only for Batch Account
 * quota issue. It can be extended by implementing new specific mapper rule if required.
 */
public class FlightExceptionTranslator {
  private final Exception flightException;
  private ArrayList<ExceptionMatchingRule> exceptionMatchingRules = new ArrayList<>();

  public FlightExceptionTranslator(Exception exception) {
    exceptionMatchingRules.add(new BatchAccountQuotaExceedExceptionRule());
    this.flightException = exception;
  }

  /**
   * Translates original exception into new exception with appropriate message
   *
   * @return New exception or original one if there are no matches
   */
  public Exception translate() {
    for (var rule : exceptionMatchingRules) {
      var matchResult = rule.match(flightException);
      if (matchResult.isPresent()) {
        return matchResult.get();
      }
    }
    return flightException;
  }
}
