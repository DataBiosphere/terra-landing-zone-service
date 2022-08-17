package bio.terra.landingzone.library.landingzones;

import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import java.util.List;

public class TestUtils {
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
