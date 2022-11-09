package bio.terra.landingzone.library.landingzones.management;

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
}
