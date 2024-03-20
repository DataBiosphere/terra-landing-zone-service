package bio.terra.landingzone.terraform.updaters;

import com.hashicorp.cdktf.TerraformResourceLifecycle;
import com.hashicorp.cdktf.TerraformStack;
import com.hashicorp.cdktf.providers.azurerm.storage_account.StorageAccount;
import java.util.List;

public class StorageAccountUpdater {

  private final TerraformStack landingZoneStack;
  private final com.azure.resourcemanager.storage.models.StorageAccount existingStorageAccount;

  private static final List IGNORED_ATTRIBUTES = List.of("tags");

  public StorageAccountUpdater(
      TerraformStack landingZoneStack,
      com.azure.resourcemanager.storage.models.StorageAccount existingStorageAccount) {
    this.landingZoneStack = landingZoneStack;
    this.existingStorageAccount = existingStorageAccount;
  }

  public void updateResource(String storageAccountName, String resourceGroupName) {

    // this demonstrates "updating" a resource, ignoring changes to properties we do not want to
    // manage (tags in this case)
    // this code should express what a storage account should look like in the given landing zone
    // terraform will take care of orchestrating any needed changes

    StorageAccount.Builder.create(landingZoneStack, "storageAccount")
        .name(existingStorageAccount.name())
        .resourceGroupName(existingStorageAccount.resourceGroupName())
        .accountReplicationType("LRS")
        .accountTier("Standard")
        .location(existingStorageAccount.regionName())
        .lifecycle(TerraformResourceLifecycle.builder().ignoreChanges(IGNORED_ATTRIBUTES).build())
        .build();
  }
}
