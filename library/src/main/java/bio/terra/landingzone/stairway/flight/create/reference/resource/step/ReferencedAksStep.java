package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksStep.AKS_OIDC_ISSUER_URL;
import static bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksStep.AKS_RESOURCE_KEY;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.stairway.FlightContext;

public class ReferencedAksStep extends SharedReferencedResourceStep {

  public ReferencedAksStep(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  protected ArmResourceType getArmResourceType() {
    return ArmResourceType.AKS;
  }

  @Override
  protected void updateWorkingMap(
      FlightContext context, ArmManagers armManagers, String resourceId) {
    var aks = armManagers.azureResourceManager().kubernetesClusters().getById(resourceId);

    context
        .getWorkingMap()
        .put(AKS_OIDC_ISSUER_URL, aks.innerModel().oidcIssuerProfile().issuerUrl());

    context
        .getWorkingMap()
        .put(
            AKS_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(aks.id())
                .resourceType(aks.type())
                .tags(aks.tags())
                .region(aks.regionName())
                .resourceName(aks.name())
                .build());
  }
}
