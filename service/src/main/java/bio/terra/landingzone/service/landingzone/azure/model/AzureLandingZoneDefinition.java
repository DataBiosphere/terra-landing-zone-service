package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.db.exception.MissingRequiredFieldsException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class AzureLandingZoneDefinition {
  private final String name;
  private final String version;
  private final Map<String, String> parameters;

  public AzureLandingZoneDefinition(String name, String version, Map<String, String> parameters) {
    this.name = name;
    this.version = version;
    this.parameters = parameters;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    AzureLandingZoneDefinition azureLandingZone = (AzureLandingZoneDefinition) o;

    return new EqualsBuilder()
        .append(name, azureLandingZone.name)
        .append(version, azureLandingZone.version)
        .append(parameters, azureLandingZone.parameters)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(name).append(version).append(parameters).toHashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String version;
    private Map<String, String> parameters;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder parameters(Map<String, String> parameters) {
      this.parameters = parameters;
      return this;
    }

    public AzureLandingZoneDefinition build() {
      if (StringUtils.isEmpty(name)) {
        throw new MissingRequiredFieldsException("Azure landing zone definition requires name");
      }
      return new AzureLandingZoneDefinition(name, version, parameters);
    }
  }
}
