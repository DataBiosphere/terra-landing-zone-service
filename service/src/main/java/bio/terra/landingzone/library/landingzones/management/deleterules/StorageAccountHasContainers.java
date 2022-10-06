package bio.terra.landingzone.library.landingzones.management.deleterules;

import static bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils.AZURE_STORAGE_ACCOUNT_TYPE;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;

public class StorageAccountHasContainers extends ResourceDependencyDeleteRule {
  public StorageAccountHasContainers(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public String getExpectedType() {
    return AZURE_STORAGE_ACCOUNT_TYPE;
  }

  @Override
  public boolean hasDependentResources(ResourceToDelete resourceToDelete) {

    return armManagers
        .azureResourceManager()
        .storageBlobContainers()
        .list(resourceToDelete.resource().resourceGroupName(), resourceToDelete.resource().name())
        .stream()
        .findAny()
        .isPresent();
  }
}
