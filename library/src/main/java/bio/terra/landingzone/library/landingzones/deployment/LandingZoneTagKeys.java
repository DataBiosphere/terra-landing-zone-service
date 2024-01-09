package bio.terra.landingzone.library.landingzones.deployment;

/** Enum of tag keys for resources in the landing zone. */
public enum LandingZoneTagKeys {
  LANDING_ZONE_ID("WLZ-ID"),
  LANDING_ZONE_PURPOSE("WLZ-PURPOSE"),
  PGBOUNCER_ENABLED("pgbouncer-enabled"),
  AKS_COST_SAVING_SPOT_NODES_ENABLED("aks-cost-spot-nodes-enabled"),
  AKS_COST_SAVING_VPA_ENABLED("aks-cost-vpa-enabled");

  private String value;

  LandingZoneTagKeys(String value) {
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
