package bio.terra.landingzone.service.landingzone.azure.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LandingZoneDefinition {
  private final String definition;
  private final String name;
  private final String description;
  private final String version;

  public LandingZoneDefinition(String definition, String name, String description, String version) {
    this.definition = definition;
    this.name = name;
    this.description = description;
    this.version = version;
  }

  public String getDefinition() {
    return this.definition;
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public String getVersion() {
    return this.version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    LandingZoneDefinition template = (LandingZoneDefinition) o;

    return new EqualsBuilder()
        .append(definition, template.definition)
        .append(name, template.name)
        .append(description, template.description)
        .append(version, template.version)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(definition)
        .append(name)
        .append(description)
        .append(version)
        .toHashCode();
  }

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
