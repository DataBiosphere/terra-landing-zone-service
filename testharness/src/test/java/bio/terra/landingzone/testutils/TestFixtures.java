package bio.terra.landingzone.testutils;

import bio.terra.landingzone.db.model.LandingZoneRecord;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TestFixtures {
  public static LandingZoneRecord createLandingZone(
      UUID landingZoneId,
      String resourceGroupId,
      String definition,
      String version,
      String displayName,
      String description,
      Map<String, String> properties,
      String subscription,
      String tenant,
      UUID billingProfileId,
      OffsetDateTime createdDate) {
    return new LandingZoneRecord(
        landingZoneId,
        resourceGroupId,
        definition,
        version,
        subscription,
        tenant,
        billingProfileId,
        createdDate,
        Optional.of(displayName),
        Optional.of(description),
        properties);
  }
}
