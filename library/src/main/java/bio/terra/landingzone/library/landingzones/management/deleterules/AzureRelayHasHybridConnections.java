package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_RELAY_TYPE;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;

public class AzureRelayHasHybridConnections extends ResourceDependencyDeleteRule {
  public AzureRelayHasHybridConnections(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public String getExpectedType() {
    return AZURE_RELAY_TYPE;
  }

  @Override
  public boolean hasDependentResources(ResourceToDelete resourceToDelete) {
    return armManagers
        .relayManager()
        .hybridConnections()
        .listByNamespace(
            resourceToDelete.resource().resourceGroupName(), resourceToDelete.resource().name())
        .stream()
        .findAny()
        .isPresent();
  }
}
