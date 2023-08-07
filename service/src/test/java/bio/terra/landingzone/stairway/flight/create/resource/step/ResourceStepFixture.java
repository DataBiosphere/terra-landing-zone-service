package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.profile.model.ProfileModel;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ResourceStepFixture {
  private ResourceStepFixture() {}

  public static TargetManagedResourceGroup createDefaultMrg() {
    return new TargetManagedResourceGroup("mgrName", "eastus");
  }

  public static ProfileModel createDefaultProfileModel() {
    return new ProfileModel()
        .id(UUID.randomUUID())
        .managedResourceGroupId("defaultManagedResourceGroupId")
        .subscriptionId(UUID.randomUUID())
        .tenantId(UUID.randomUUID());
  }

  public static LandingZoneResource createAksLandingZoneResource(String aksId, String aksName) {
    return new LandingZoneResource(
        aksId, "aks", Map.of(), "eastus", Optional.of(aksName), Optional.empty());
  }
}
