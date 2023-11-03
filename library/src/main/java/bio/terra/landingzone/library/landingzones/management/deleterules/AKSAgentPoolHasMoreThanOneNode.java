package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_AKS_TYPE;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;

public class AKSAgentPoolHasMoreThanOneNode extends ResourceDependencyDeleteRule {
  public AKSAgentPoolHasMoreThanOneNode(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public String getExpectedType() {
    return AZURE_AKS_TYPE;
  }

  @Override
  public boolean hasDependentResources(ResourceToDelete resourceToDelete) {
    return armManagers
        .azureResourceManager()
        .kubernetesClusters()
        .getByResourceGroup(
            resourceToDelete.resource().resourceGroupName(), resourceToDelete.resource().name())
        .agentPools()
        .values()
        .stream()
        .anyMatch(k -> k.count() > 1);
  }
}
