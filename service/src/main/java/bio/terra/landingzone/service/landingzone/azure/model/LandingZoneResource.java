package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.Map;

public record LandingZoneResource(
    String resourceId, String resourceType, Map<String, String> tags, String region) {

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
