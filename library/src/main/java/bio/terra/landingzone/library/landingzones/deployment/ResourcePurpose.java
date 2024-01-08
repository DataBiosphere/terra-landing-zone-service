package bio.terra.landingzone.library.landingzones.deployment;

/** Enum that indicates the purpose of the resource in the landing zone. */
public enum ResourcePurpose implements LandingZonePurpose {
  SHARED_RESOURCE("SHARED_RESOURCE"),
  POSTGRES_ADMIN("POSTGRES_ADMIN"),
  WLZ_RESOURCE("WLZ_RESOURCE");

  private String value;

  ResourcePurpose(String value) {
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
