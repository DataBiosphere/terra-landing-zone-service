package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_VNET_TYPE;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;

public class VmsAreAttachedToVnet extends ResourceDependencyDeleteRule {

  public VmsAreAttachedToVnet(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public String getExpectedType() {
    return AZURE_VNET_TYPE;
  }

  @Override
  public boolean hasDependentResources(ResourceToDelete resourceToDelete) {

    return armManagers
        .azureResourceManager()
        .virtualMachines()
        .listByResourceGroup(resourceToDelete.resource().resourceGroupName())
        .stream()
        .anyMatch(
            v ->
                v.getPrimaryNetworkInterface()
                    .primaryIPConfiguration()
                    .getNetwork()
                    .id()
                    .equalsIgnoreCase(resourceToDelete.resource().id()));
  }
}
