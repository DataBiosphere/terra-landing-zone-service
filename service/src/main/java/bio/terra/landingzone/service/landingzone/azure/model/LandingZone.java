package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public record LandingZone(
    UUID landingZoneId, UUID billingProfileId, String definition, String version) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String definition;
    private String version;
    private UUID landingZoneId;
    private UUID billingProfileId;

    public Builder landingZoneId(UUID landingZoneId) {
      this.landingZoneId = landingZoneId;
      return this;
    }

    public Builder billingProfileId(UUID billingProfileId) {
      this.billingProfileId = billingProfileId;
      return this;
    }

    public Builder definition(String definition) {
      this.definition = definition;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public LandingZone build() {
      if (landingZoneId == null) {
        throw new MissingRequiredFieldsException(
            "Azure landing zone record requires landing zone ID");
      }
      if (billingProfileId == null) {
        throw new MissingRequiredFieldsException(
            "Azure landing zone definition requires billing profile ID");
      }
      if (StringUtils.isBlank(definition)) {
        throw new MissingRequiredFieldsException("Azure landing zone record requires definition");
      }
      if (StringUtils.isBlank(version)) {
        throw new MissingRequiredFieldsException("Azure landing zone record requires version");
      }
      return new LandingZone(landingZoneId, billingProfileId, definition, version);
    }
  }
}
