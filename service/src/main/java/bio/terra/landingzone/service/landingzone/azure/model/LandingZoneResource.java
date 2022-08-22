package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LandingZoneResource {
  private final String resourceId;
  private final String resourceType;
  private final Map<String, String> tags;
  private final String region;

  public LandingZoneResource(
      String resourceId, String resourceType, Map<String, String> tags, String region) {
    this.resourceId = resourceId;
    this.resourceType = resourceType;
    this.tags = tags;
    this.region = region;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public String getRegion() {
    return region;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    LandingZoneResource resource = (LandingZoneResource) o;

    return new EqualsBuilder()
        .append(resourceId, resource.resourceId)
        .append(resourceType, resource.resourceType)
        .append(tags, resource.tags)
        .append(region, resource.region)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(resourceId)
        .append(resourceType)
        .append(tags)
        .append(region)
        .toHashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String resourceId;
    private String resourceType;
    private Map<String, String> tags;
    private String region;

    public Builder resourceId(String resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public Builder resourceType(String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder tags(Map<String, String> tags) {
      this.tags = tags;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public LandingZoneResource build() {
      return new LandingZoneResource(resourceId, resourceType, tags, region);
    }
  }
}
