package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;

public class ResourceStepFixture {
  private ResourceStepFixture() {}

  public static TargetManagedResourceGroup createDefaultMrg() {
    return new TargetManagedResourceGroup("mgrName", "mrgRegion");
  }
}
