package bio.terra.landingzone.testutils;

import bio.terra.landingzone.db.model.LandingZone;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TestFixtures {
  public static LandingZone createLandingZone(
      UUID landingZoneId,
      String resourceGroupId,
      String definition,
      String version,
      String displayName,
      String description,
      Map<String, String> properties,
      String subscription,
      String tenant,
      UUID billingProfileId) {
    return new LandingZone(
        landingZoneId,
        resourceGroupId,
        definition,
        version,
        subscription,
        tenant,
        billingProfileId,
        Optional.of(displayName),
        Optional.of(description),
        properties);
  }
}
