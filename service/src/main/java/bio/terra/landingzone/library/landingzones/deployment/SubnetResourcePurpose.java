package bio.terra.landingzone.library.landingzones.deployment;

import com.azure.core.util.ExpandableStringEnum;
import java.util.Collection;

/** Enum of the subnet purposes */
public final class SubnetResourcePurpose extends ExpandableStringEnum<SubnetResourcePurpose> {
  public static final SubnetResourcePurpose WORKSPACE_COMPUTE_SUBNET =
      fromString("WORKSPACE_COMPUTE_SUBNET");
  public static final SubnetResourcePurpose WORKSPACE_STORAGE_SUBNET =
      fromString("WORKSPACE_STORAGE_SUBNET");
  public static final SubnetResourcePurpose AKS_NODE_POOL_SUBNET =
      fromString("AKS_NODE_POOL_SUBNET");
  public static final SubnetResourcePurpose POSTGRESQL_SUBNET = fromString("POSTGRESQL_SUBNET");
  public static final SubnetResourcePurpose WORKSPACE_BATCH_SUBNET =
      fromString("WORKSPACE_BATCH_SUBNET");
  /**
   * Creates or finds a {@link SubnetResourcePurpose} from its string representation.
   *
   * @param name a name to look for
   * @return the corresponding {@link SubnetResourcePurpose}
   */
  public static SubnetResourcePurpose fromString(String name) {
    return fromString(name, SubnetResourcePurpose.class);
  }

  public static Collection<SubnetResourcePurpose> values() {
    return values(SubnetResourcePurpose.class);
  }
}
