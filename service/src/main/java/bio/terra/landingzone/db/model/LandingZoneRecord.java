package bio.terra.landingzone.db.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/** Internal representation of a Landing Zone. */
@JsonDeserialize(builder = LandingZoneRecord.Builder.class)
public record LandingZoneRecord(
    UUID landingZoneId,
    String resourceGroupId,
    String definition,
    String version,
    String subscriptionId,
    String tenantId,
    UUID billingProfileId,
    OffsetDateTime createdDate,
    Optional<String> displayName,
    Optional<String> description,
    Map<String, String> properties) {

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private UUID landingZoneId;
    private String resourceGroupId;
    private String definition;
    private String version;
    private String subscriptionId;
    private String tenantId;
    private UUID billingProfileId;
    private @Nullable String displayName;
    private @Nullable String description;
    private OffsetDateTime createdDate;
    private Map<String, String> properties;

    public Builder landingZoneId(UUID landingZoneUuid) {
      this.landingZoneId = landingZoneUuid;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder resourceGroupId(String resourceGroupId) {
      this.resourceGroupId = resourceGroupId;
      return this;
    }

    public Builder properties(Map<String, String> properties) {
      this.properties = properties;
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

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder subscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
      return this;
    }

    public Builder billingProfileId(UUID billingProfileId) {
      this.billingProfileId = billingProfileId;
      return this;
    }

    public Builder createdDate(OffsetDateTime createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public LandingZoneRecord build() {
      // Always have a map, even if it is empty
      if (properties == null) {
        properties = new HashMap<>();
      }
      if (displayName == null) {
        displayName = "";
      }
      if (description == null) {
        description = "";
      }
      if (landingZoneId == null) {
        throw new MissingRequiredFieldsException("Landing zone requires id");
      }
      return new LandingZoneRecord(
          landingZoneId,
          resourceGroupId,
          definition,
          version,
          subscriptionId,
          tenantId,
          billingProfileId,
          createdDate,
          Optional.of(displayName),
          Optional.of(description),
          properties);
    }
  }
}
