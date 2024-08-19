package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;

public class ReferencedAppInsightsStep extends SharedReferencedResourceStep {
    public ReferencedAppInsightsStep(ArmManagers armManagers) {
        super(armManagers);
    }

    @Override
    protected ArmResourceType getArmResourceType() {
        return ArmResourceType.APP_INSIGHTS;
    }
}
