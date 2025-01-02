package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

public enum ArmResourceType {
  AKS("Microsoft.ContainerService/managedClusters"),
  VNET("Microsoft.Network/virtualNetworks"),
  STORAGE_ACCOUNT("Microsoft.Storage/storageAccounts"),
  POSTGRES_FLEXIBLE("Microsoft.DBforPostgreSQL/flexibleServers"),
  BATCH("Microsoft.Batch/batchAccounts"),
  APP_INSIGHTS("Microsoft.Insights/components"),
  MANAGED_IDENTITY("Microsoft.ManagedIdentity/userAssignedIdentities"),
  PRIVATE_DNS_ZONE("Microsoft.Network/privateDnsZones"),
  RELAY_NAMESPACE("Microsoft.Relay/namespaces");

  private final String value;

  ArmResourceType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
