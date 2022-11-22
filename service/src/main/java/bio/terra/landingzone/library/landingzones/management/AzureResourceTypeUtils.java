package bio.terra.landingzone.library.landingzones.management;

import org.apache.commons.lang3.StringUtils;

public class AzureResourceTypeUtils {
  private AzureResourceTypeUtils() {}

  public static final String AZURE_VNET_TYPE = "Microsoft.Network/virtualNetworks";
  public static final String AZURE_RELAY_TYPE = "Microsoft.Relay/namespaces";

  public static final String AZURE_STORAGE_ACCOUNT_TYPE = "Microsoft.Storage/storageAccounts";
  public static final String AZURE_POSTGRESQL_SERVER_TYPE = "Microsoft.DBforPostgreSQL/servers";
  public static final String AZURE_AKS_TYPE = "Microsoft.ContainerService/managedClusters";
  public static final String AZURE_BATCH_TYPE = "Microsoft.Batch/batchAccounts";
  public static final String AZURE_LOG_ANALYTICS_WORKSPACE_TYPE =
      "Microsoft.OperationalInsights/workspaces";
  public static final String AZURE_SOLUTIONS_TYPE = "Microsoft.OperationsManagement/solutions";

  public static final String RESOURCE_ID_FORMAT =
      "subscriptions/{guid}/resourceGroups/{resource-group-name}/{resource-provider-namespace}/{resource-type}/{resource-name}";

  public static final String resourceTypeFromResourceId(String resourceID) {
    if (StringUtils.isBlank(resourceID)) {
      throw new IllegalArgumentException("resource id is blank");
    }

    String[] segments = StringUtils.split(StringUtils.strip(resourceID.trim(), "/"), "/");

    // there should be 8 segments in a valid resource id.
    if (segments.length != 8) {
      throw new IllegalArgumentException(
          "The resource id is not in the correct format. The format must be:" + RESOURCE_ID_FORMAT);
    }

    // the second to last segment is the resource provider
    return String.format("%s/%s", segments[5], segments[6]);
  }
}
