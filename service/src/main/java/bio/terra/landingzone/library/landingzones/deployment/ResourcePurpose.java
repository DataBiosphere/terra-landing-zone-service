package bio.terra.landingzone.library.landingzones.deployment;

import com.azure.core.util.ExpandableStringEnum;
import java.util.Collection;

/** Enum that indicates the purpose of the resource in the landing zone. */
public final class ResourcePurpose extends ExpandableStringEnum<ResourcePurpose>
    implements LandingZonePurpose {

  public static final ResourcePurpose SHARED_RESOURCE = fromString("SHARED_RESOURCE");
  public static final ResourcePurpose POSTGRES_ADMIN = fromString("POSTGRES_ADMIN");
  public static final ResourcePurpose WLZ_RESOURCE = fromString("WLZ_RESOURCE");

  /**
   * Creates or finds a {@link ResourcePurpose} from its string representation.
   *
   * @param name a name to look for
   * @return the corresponding {@link ResourcePurpose}
   */
  public static ResourcePurpose fromString(String name) {
    return fromString(name, ResourcePurpose.class);
  }

  public static Collection<ResourcePurpose> values() {
    return values(ResourcePurpose.class);
  }
}
