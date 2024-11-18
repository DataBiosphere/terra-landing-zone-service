package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.stairway.FlightContext;

import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksStep.AKS_OIDC_ISSUER_URL;
import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksStep.AKS_RESOURCE_KEY;

public class ReferencedAksStep extends SharedReferencedResourceStep {

  public ReferencedAksStep(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  protected ArmResourceType getArmResourceType() {
    return ArmResourceType.AKS;
  }

  @Override
  protected void updateWorkingMap(FlightContext context, ArmManagers armManagers, String resourceId)
  {
    var aks = armManagers
            .azureResourceManager()
            .kubernetesClusters()
            .getById(resourceId);

    context
            .getWorkingMap()
            .put(AKS_OIDC_ISSUER_URL, aks.innerModel().oidcIssuerProfile().issuerUrl());

    context
            .getWorkingMap()
            .put(AKS_RESOURCE_KEY, resourceId);
  }
}
