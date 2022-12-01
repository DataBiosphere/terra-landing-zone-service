package bio.terra.landingzone.stairway.flight.exception.translation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class BatchAccountQuotaExceedExceptionRuleTest {
  private BatchAccountQuotaExceedExceptionRule batchQuotaAccountExceedRule;

  @BeforeEach
  void setUp() {
    batchQuotaAccountExceedRule = new BatchAccountQuotaExceedExceptionRule();
  }

  @Test
  void successfullyMapBatchAccountQuotaException() {
    var flightException = ExceptionFixtures.createBatchAccountQuotaException();

    Optional<LandingZoneCreateException> mappedException =
        batchQuotaAccountExceedRule.match(flightException);

    assertTrue(mappedException.isPresent());
    assertTrue(
        mappedException
            .get()
            .getMessage()
            .contains(BatchAccountQuotaExceedExceptionRule.BATCH_QUOTA_MESSAGE_MARKER));
  }

  @Test
  void originalExceptionIsNotMatched() {
    var flightException =
        new ManagementException(
            "not matched message",
            null,
            new ManagementError("code", "not matched internal message"));

    assertTrue(batchQuotaAccountExceedRule.match(flightException).isEmpty());
  }
}
