package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class AzureLandingZoneRequest {
  private final String definition;
  private final String version;
  private final Map<String, String> parameters;

  public AzureLandingZoneRequest(
      String definition, String version, Map<String, String> parameters) {
    this.definition = definition;
    this.version = version;
    this.parameters = parameters;
  }

  public String getDefinition() {
    return definition;
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

    AzureLandingZoneRequest azureLandingZone = (AzureLandingZoneRequest) o;

    return new EqualsBuilder()
        .append(definition, azureLandingZone.definition)
        .append(version, azureLandingZone.version)
        .append(parameters, azureLandingZone.parameters)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(definition)
        .append(version)
        .append(parameters)
        .toHashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String definition;
    private String version;
    private Map<String, String> parameters;

    public Builder definition(String definition) {
      this.definition = definition;
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

    public AzureLandingZoneRequest build() {
      if (StringUtils.isEmpty(definition)) {
        throw new MissingRequiredFieldsException(
            "Azure landing zone definition requires definition");
      }
      return new AzureLandingZoneRequest(definition, version, parameters);
    }
  }
}
