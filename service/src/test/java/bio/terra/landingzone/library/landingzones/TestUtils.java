package bio.terra.landingzone.library.landingzones;

import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import java.util.List;

public class TestUtils {

  public static final String STUB_BATCH_ACCOUNT_ID =
      "subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/qms-test/providers/Microsoft.Batch/batchAccounts/testAccount";
  public static final String STUB_AKS_ID =
      "subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/mrg-20221025205925/providers/Microsoft.ContainerService/managedClusters/akscluster";

  public static String findFirstStorageAccountId(List<DeployedResource> resources) {
    return findFirstResourceIdByResourceType(resources, "Microsoft.Storage/storageAccounts");
  }

  public static String findFirstVNetId(List<DeployedResource> resources) {
    return findFirstResourceIdByResourceType(resources, "Microsoft.Network/virtualNetworks");
  }

  public static String findFirstResourceIdByResourceType(
      List<DeployedResource> resources, String type) {
    return resources.stream()
        .filter(r -> r.resourceType().equalsIgnoreCase(type))
        .findFirst()
        .orElse(null)
        .resourceId();
  }
}
