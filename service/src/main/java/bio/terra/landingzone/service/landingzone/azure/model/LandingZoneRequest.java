package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 * Data structure which incorporates parameters for landing zone creation.
 *
 * @param definition Defines the shape of the landing zone (which resources will be deployed).
 * @param version Version of the definition.
 * @param parameters Parameters used if we need to customize certain resource.
 * @param billingProfileId Unique identifier of a billing profile.
 * @param landingZoneId Optional value for unique identifier of a landing zone.
 * @param useStairwayPath When useStairwayPath parameter is explicitly set to True we initiate
 *     alternative way of Landing Zone resources creation.
 */
public record LandingZoneRequest(
    String definition,
    String version,
    Map<String, String> parameters,
    UUID billingProfileId,
    Optional<UUID> landingZoneId,
    Boolean useStairwayPath) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String definition;
    private String version;
    private Map<String, String> parameters;
    private UUID billingProfileId;
    private UUID landingZoneId;
    private Boolean useStairwayPath;

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

    public Builder useStairwayPath(Boolean useStairwayPath) {
      this.useStairwayPath = useStairwayPath;
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

      if (useStairwayPath == null) {
        useStairwayPath = false;
      }

      return new LandingZoneRequest(
          definition,
          version,
          parameters,
          billingProfileId,
          Optional.ofNullable(landingZoneId),
          useStairwayPath);
    }
  }

  public boolean isAttaching() {
    if (null == this.parameters) {
      return false;
    }

    return Boolean.parseBoolean(
        this.parameters().getOrDefault(LandingZoneFlightMapKeys.ATTACH, "false"));
  }
}
