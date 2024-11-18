package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneIdentityStep;
import bio.terra.stairway.FlightContext;

import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_CLIENT_ID;
import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY;

public class ReferencedManagedIdentityStep extends SharedReferencedResourceStep {
    public ReferencedManagedIdentityStep(ArmManagers armManagers) {
        super(armManagers);
    }

    @Override
    protected ArmResourceType getArmResourceType() {
        return ArmResourceType.MANAGED_IDENTITY;
    }

    @Override
    protected void updateWorkingMap(FlightContext context, ArmManagers armManagers, String resourceId)
    {
        var id = armManagers
                .azureResourceManager()
                .identities()
                .getById(resourceId);

        context
                .getWorkingMap()
                .put(LANDING_ZONE_IDENTITY_CLIENT_ID, id.innerModel().clientId());

        context
                .getWorkingMap()
                .put(LANDING_ZONE_IDENTITY_RESOURCE_KEY, resourceId);
    }
}
