package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class ReferencedManagedIdentityStep extends SharedReferencedResourceStep {
    public ReferencedManagedIdentityStep(ArmManagers armManagers) {
        super(armManagers);
    }

    @Override
    protected ArmResourceType getArmResourceType() {
        return ArmResourceType.MANAGED_IDENTITY;
    }
}
