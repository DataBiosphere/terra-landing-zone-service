package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class ReferencedBatchStep extends SharedReferencedResourceStep {
    public ReferencedBatchStep(ArmManagers armManagers) {
        super(armManagers);
    }

    @Override
    protected ArmResourceType getArmResourceType() {
        return ArmResourceType.BATCH;
    }
}
