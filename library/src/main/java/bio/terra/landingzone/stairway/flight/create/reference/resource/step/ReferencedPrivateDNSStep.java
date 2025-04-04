package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class ReferencedPrivateDNSStep extends SharedReferencedResourceStep {
  public ReferencedPrivateDNSStep(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  protected ArmResourceType getArmResourceType() {
    return ArmResourceType.PRIVATE_DNS_ZONE;
  }
}
