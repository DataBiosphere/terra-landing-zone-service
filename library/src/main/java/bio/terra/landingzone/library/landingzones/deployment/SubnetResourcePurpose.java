package bio.terra.landingzone.library.landingzones.deployment;

/** Enum of the subnet purposes */
public enum SubnetResourcePurpose implements LandingZonePurpose {
  WORKSPACE_COMPUTE_SUBNET("WORKSPACE_COMPUTE_SUBNET"),
  WORKSPACE_STORAGE_SUBNET("WORKSPACE_STORAGE_SUBNET"),
  AKS_NODE_POOL_SUBNET("AKS_NODE_POOL_SUBNET"),
  POSTGRESQL_SUBNET("POSTGRESQL_SUBNET"),
  WORKSPACE_BATCH_SUBNET("WORKSPACE_BATCH_SUBNET");

  private String value;

  SubnetResourcePurpose(String value) {
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
