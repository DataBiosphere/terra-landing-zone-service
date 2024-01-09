package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.profile.model.ProfileModel;
import com.azure.core.management.profile.AzureProfile;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LandingZoneRequestFixtures {
  private LandingZoneRequestFixtures() {}

  public static LandingZoneRequest createCromwellLZRequest(
      UUID landingZoneId, UUID billingProfileId) {
    return new LandingZoneRequest(
        StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue(),
        "v1",
        Map.of(),
        billingProfileId,
        Optional.of(landingZoneId));
  }

  public static LandingZoneRequest createProtectedDataLZRequest(
      UUID landingZoneId, UUID billingProfileId) {
    return new LandingZoneRequest(
        StepsDefinitionFactoryType.PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME.getValue(),
        "v1",
        Map.of(),
        billingProfileId,
        Optional.of(landingZoneId));
  }

  public static LandingZoneRecord createLandingZoneRecord(
      UUID landingZoneId,
      String resourceGroupName,
      AzureProfile azureProfile,
      ProfileModel profile) {
    return new LandingZoneRecord(
        landingZoneId,
        resourceGroupName,
        "definition",
        "version",
        azureProfile.getSubscriptionId(),
        azureProfile.getTenantId(),
        profile.getId(),
        null,
        OffsetDateTime.now(),
        Optional.of("displayName"),
        Optional.of("name"),
        Map.of());
  }
}
