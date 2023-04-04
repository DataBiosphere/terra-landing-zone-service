package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record DeletedLandingZone(
    UUID landingZoneId, List<String> deleteResources, UUID billingProfileId) {

  public static DeletedLandingZone emptyLandingZone(UUID landingZoneId, UUID billingProfileId) {
    return new DeletedLandingZone(landingZoneId, Collections.emptyList(), billingProfileId);
  }
}
