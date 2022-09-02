package bio.terra.landingzone.db.model;

import bio.terra.landingzone.common.exception.MissingRequiredFieldsException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/** Internal representation of a Landing Zone. */
@JsonDeserialize(builder = LandingZone.Builder.class)
public class LandingZone {
  private final UUID landingZoneId;
  private final String resourceGroupId;
  private final String description;
  private final String displayName;
  private final String definition;
  private final String version;
  private final Map<String, String> properties;
  private final String subscriptionId;
  private final String tenantId;

  public LandingZone(
      UUID landingZoneId,
      String resourceGroupId,
      String definition,
      String version,
      String displayName,
      String description,
      Map<String, String> properties,
      String subscriptionId,
      String tenantId) {
    this.landingZoneId = landingZoneId;
    this.resourceGroupId = resourceGroupId;
    this.definition = definition;
    this.version = version;
    this.displayName = displayName;
    this.description = description;
    this.tenantId = tenantId;
    this.subscriptionId = subscriptionId;
    this.properties = properties;
  }

  /** The globally unique identifier of this landing zone. */
  public UUID getLandingZoneId() {
    return landingZoneId;
  }

  public String getResourceGroupId() {
    return resourceGroupId;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getDefinition() {
    return definition;
  }

  public String getVersion() {
    return version;
  }

  /** Optional display name for the landing zone. */
  public Optional<String> getDisplayName() {
    return Optional.ofNullable(displayName);
  }

  /** Optional description of the landing zone. */
  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  /** Caller-specified set of key-value pairs */
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    LandingZone landingZone = (LandingZone) o;

    return new EqualsBuilder()
        .append(landingZoneId, landingZone.landingZoneId)
        .append(resourceGroupId, landingZone.resourceGroupId)
        .append(definition, landingZone.definition)
        .append(version, landingZone.version)
        .append(displayName, landingZone.displayName)
        .append(subscriptionId, landingZone.subscriptionId)
        .append(tenantId, landingZone.tenantId)
        .append(description, landingZone.description)
        .append(properties, landingZone.properties)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(landingZoneId)
        .append(resourceGroupId)
        .append(definition)
        .append(version)
        .append(subscriptionId)
        .append(tenantId)
        .append(displayName)
        .append(description)
        .append(properties)
        .toHashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private UUID landingZoneId;
    private String resourceGroupId;
    private String definition;
    private String version;
    private String displayName;
    private String subscriptionId;
    private String tenantId;
    private @Nullable String description;
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

    public LandingZone build() {
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
      return new LandingZone(
          landingZoneId,
          resourceGroupId,
          definition,
          version,
          displayName,
          description,
          properties,
          subscriptionId,
          tenantId);
    }
  }
}
