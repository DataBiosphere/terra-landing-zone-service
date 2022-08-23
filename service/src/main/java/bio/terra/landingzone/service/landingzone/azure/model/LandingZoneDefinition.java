package bio.terra.landingzone.service.landingzone.azure.model;

public record LandingZoneDefinition(
    String definition, String name, String description, String version) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String definition;
    private String name;
    private String description;
    private String version;

    public Builder definition(String definition) {
      this.definition = definition;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public LandingZoneDefinition build() {
      return new LandingZoneDefinition(definition, name, description, version);
    }
  }
}
