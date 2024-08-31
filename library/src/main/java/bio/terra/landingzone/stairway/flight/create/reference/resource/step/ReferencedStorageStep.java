package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class ReferencedStorageStep extends SharedReferencedResourceStep {
  public ReferencedStorageStep(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  protected ArmResourceType getArmResourceType() {
    return ArmResourceType.STORAGE_ACCOUNT;
  }
}
