package bio.terra.landingzone.library.landingzones.definition;

/** Enum representing the list of possible versions for a given definition. */
public enum DefinitionVersion {
  V1("v1"),
  V2("v2"),
  V3("v3"),
  V4("v4"),
  V5("v5");

  private String value;

  DefinitionVersion(String value) {
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
