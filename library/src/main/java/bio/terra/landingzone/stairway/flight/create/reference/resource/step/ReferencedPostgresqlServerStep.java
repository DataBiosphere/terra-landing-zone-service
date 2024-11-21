package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class ReferencedPostgresqlServerStep extends SharedReferencedResourceStep {
  public ReferencedPostgresqlServerStep(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  protected ArmResourceType getArmResourceType() {
    return ArmResourceType.POSTGRES_FLEXIBLE;
  }
}
