package bio.terra.landingzone.terraform;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.library.AzureCredentialsProvider;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.storage.models.StorageAccount;

public class ResourceResolver {

  private LandingZoneService landingZoneService;
  private AzureCredentialsProvider azureCredentialsProvider;

  public ResourceResolver(
      LandingZoneService landingZoneService, AzureCredentialsProvider azureCredentialsProvider) {
    this.landingZoneService = landingZoneService;
    this.azureCredentialsProvider = azureCredentialsProvider;
  }

  public StorageAccount getLandingZoneStorageAccount(
      LandingZoneRecord record, BearerToken bearerToken) {
    var resources =
        landingZoneService.listResourcesByPurpose(
            bearerToken, record.landingZoneId(), ResourcePurpose.SHARED_RESOURCE);
    var accts =
        resources.stream()
            .filter(r -> r.resourceType().equals("Microsoft.Storage/storageAccounts"))
            .toList();
    if (accts.size() != 1) {
      throw new RuntimeException("Expected 1 storage account, found " + accts.size());
    }

    var azureProfile =
        new AzureProfile(record.tenantId(), record.subscriptionId(), AzureEnvironment.AZURE);
    var armManagers =
        LandingZoneManager.createArmManagers(
            azureCredentialsProvider.getTokenCredential(), azureProfile, "ignore");

    return armManagers.azureResourceManager().storageAccounts().getById(accts.get(0).resourceId());
  }

  public KubernetesCluster getLandingZoneAks(LandingZoneRecord record, BearerToken bearerToken) {
    var resources =
        landingZoneService.listResourcesByPurpose(
            bearerToken, record.landingZoneId(), ResourcePurpose.SHARED_RESOURCE);
    var aks =
        resources.stream()
            .filter(r -> r.resourceType().equals("Microsoft.ContainerService/managedClusters"))
            .toList();
    if (aks.size() != 1) {
      throw new RuntimeException("Expected 1 aks cluster, found " + aks.size());
    }

    var azureProfile =
        new AzureProfile(record.tenantId(), record.subscriptionId(), AzureEnvironment.AZURE);
    var armManagers =
        LandingZoneManager.createArmManagers(
            azureCredentialsProvider.getTokenCredential(), azureProfile, "ignore");

    return armManagers.azureResourceManager().kubernetesClusters().getById(aks.get(0).resourceId());
  }
}
