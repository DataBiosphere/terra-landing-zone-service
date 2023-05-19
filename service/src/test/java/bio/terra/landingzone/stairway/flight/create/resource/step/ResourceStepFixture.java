package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.profile.model.ProfileModel;
import java.util.UUID;

public class ResourceStepFixture {
  private ResourceStepFixture() {}

  public static TargetManagedResourceGroup createDefaultMrg() {
    return new TargetManagedResourceGroup("mgrName", "mrgRegion");
  }

  public static ProfileModel createDefaultProfileModel() {
    return new ProfileModel()
        .id(UUID.randomUUID())
        .managedResourceGroupId("defaultManagedResourceGroupId")
        .subscriptionId(UUID.randomUUID())
        .tenantId(UUID.randomUUID());
  }
}
