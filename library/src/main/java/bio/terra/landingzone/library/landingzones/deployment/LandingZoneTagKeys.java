package bio.terra.landingzone.library.landingzones.deployment;

import com.azure.core.util.ExpandableStringEnum;
import java.util.Collection;

/** Enum of tag keys for resources in the landing zone. */
public final class LandingZoneTagKeys extends ExpandableStringEnum<LandingZoneTagKeys> {
  public static final LandingZoneTagKeys LANDING_ZONE_ID = fromString("WLZ-ID");
  public static final LandingZoneTagKeys LANDING_ZONE_PURPOSE = fromString("WLZ-PURPOSE");
  public static final LandingZoneTagKeys PGBOUNCER_ENABLED = fromString("pgbouncer-enabled");
  public static final LandingZoneTagKeys AKS_COST_SAVING_SPOT_NODES_ENABLED =
      fromString("aks-cost-spot-nodes-enabled");

  /**
   * Creates or finds a {@link LandingZoneTagKeys} from its string representation.
   *
   * @param name a name to look for
   * @return the corresponding {@link LandingZoneTagKeys}
   */
  public static LandingZoneTagKeys fromString(String name) {
    return fromString(name, LandingZoneTagKeys.class);
  }

  /**
   * @return known LandingZoneTagKeys values.
   */
  public static Collection<LandingZoneTagKeys> values() {
    return values(LandingZoneTagKeys.class);
  }
}
