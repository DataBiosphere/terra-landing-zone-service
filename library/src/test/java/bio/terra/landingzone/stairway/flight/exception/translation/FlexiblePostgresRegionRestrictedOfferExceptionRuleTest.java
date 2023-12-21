package bio.terra.landingzone.stairway.flight.exception.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class FlexiblePostgresRegionRestrictedOfferExceptionRuleTest {
  private FlexiblePostgresRegionRestrictedOfferExceptionRule rule;

  @BeforeEach
  void setUp() {
    rule = new FlexiblePostgresRegionRestrictedOfferExceptionRule();
  }

  @Test
  void successfullyMapBatchAccountQuotaException() {
    var flightException = ExceptionFixtures.createFlexiblePostgresRegionRestrictionException();

    Optional<LandingZoneCreateException> mappedException = rule.match(flightException);

    assertTrue(mappedException.isPresent());
    assertEquals(flightException, mappedException.get().getCause());
    assertTrue(
        mappedException
            .get()
            .getMessage()
            .contains(
                FlexiblePostgresRegionRestrictedOfferExceptionRule
                    .FLEXIBLE_POSTGRES_REGION_RESTRICTION_MESSAGE_MARKER));
  }

  @Test
  void originalExceptionIsNotMatched() {
    var flightException =
        new ManagementException(
            "not matched message",
            null,
            new ManagementError("code", "not matched internal message"));

    assertTrue(rule.match(flightException).isEmpty());
  }
}
