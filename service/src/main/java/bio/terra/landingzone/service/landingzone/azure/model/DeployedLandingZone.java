package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.List;

public record DeployedLandingZone(String id, List<LandingZoneResource> deployedResources) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private List<LandingZoneResource> deployedResources;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder deployedResources(List<LandingZoneResource> deployedResources) {
      this.deployedResources = deployedResources;
      return this;
    }

    public DeployedLandingZone build() {
      return new DeployedLandingZone(id, deployedResources);
    }
  }
}
