package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_BATCH_TYPE;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;

public class BatchAccountHasNodePools extends ResourceDependencyDeleteRule {
  public BatchAccountHasNodePools(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public String getExpectedType() {
    return AZURE_BATCH_TYPE;
  }

  @Override
  public boolean hasDependentResources(ResourceToDelete resourceToDelete) {
    return armManagers
        .batchManager()
        .pools()
        .listByBatchAccount(
            resourceToDelete.resource().resourceGroupName(), resourceToDelete.resource().name())
        .stream()
        .findAny()
        .isPresent();
  }
}
