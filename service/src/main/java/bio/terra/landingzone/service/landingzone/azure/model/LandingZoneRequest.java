package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public record LandingZoneRequest(
    String definition,
    String version,
    Map<String, String> parameters,
    UUID billingProfileId,
    Optional<UUID> landingZoneId) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String definition;
    private String version;
    private Map<String, String> parameters;
    private UUID billingProfileId;
    private UUID landingZoneId;

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

    public Builder billingProfileId(UUID billingProfileId) {
      this.billingProfileId = billingProfileId;
      return this;
    }

    public Builder landingZoneId(UUID landingZoneId) {
      this.landingZoneId = landingZoneId;
      return this;
    }

    public LandingZoneRequest build() {
      if (StringUtils.isBlank(definition)) {
        throw new MissingRequiredFieldsException(
            "Azure landing zone definition requires definition");
      }

      if (billingProfileId == null) {
        throw new MissingRequiredFieldsException(
            "Azure landing zone definition requires billing profile ID");
      }

      return new LandingZoneRequest(
          definition, version, parameters, billingProfileId, Optional.ofNullable(landingZoneId));
    }
  }
}
