package bio.terra.landingzone.library.landingzones.management;

import com.azure.core.util.ExpandableStringEnum;
import java.util.Collection;

public final class LandingZoneBaseResourceType
    extends ExpandableStringEnum<LandingZoneBaseResourceType> {
  public static final LandingZoneBaseResourceType AZURE_VNET =
      fromString("microsoft.network/virtualnetworks");
  public static final LandingZoneBaseResourceType AZURE_PRIVATE_DNS_ZONE =
      fromString("microsoft.network/privatednszones");

  public static LandingZoneBaseResourceType fromString(String name) {
    return fromString(name, LandingZoneBaseResourceType.class);
  }

  public static boolean containsValueIgnoringCase(String value) {
    return values().stream().anyMatch(v -> v.toString().equalsIgnoreCase(value));
  }

  public static Collection<LandingZoneBaseResourceType> values() {
    return values(LandingZoneBaseResourceType.class);
  }
}
