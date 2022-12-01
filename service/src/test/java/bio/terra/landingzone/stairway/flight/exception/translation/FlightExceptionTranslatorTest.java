package bio.terra.landingzone.stairway.flight.exception.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class FlightExceptionTranslatorTest {
  private FlightExceptionTranslator flightExceptionTranslator;

  @Test
  void successfullyTranslateBatchAccountQuotaException() {
    var flightException = ExceptionFixtures.createBatchAccountQuotaException();

    flightExceptionTranslator = new FlightExceptionTranslator(flightException);
    var translatedException = flightExceptionTranslator.translate();

    assertTrue(translatedException instanceof LandingZoneCreateException);
    assertTrue(
        translatedException
            .getMessage()
            .contains(BatchAccountQuotaExceedExceptionRule.BATCH_QUOTA_MESSAGE_MARKER));
  }

  @Test
  void originalExceptionIsNotMatched() {
    var flightException = ExceptionFixtures.createNonMatchingManagementException();

    flightExceptionTranslator = new FlightExceptionTranslator(flightException);
    var translatedException = flightExceptionTranslator.translate();

    assertNotNull(translatedException);
    assertEquals(flightException, translatedException);
  }
}
