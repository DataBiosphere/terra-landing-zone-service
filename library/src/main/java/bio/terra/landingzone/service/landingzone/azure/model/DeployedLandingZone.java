package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.List;
import java.util.UUID;

public record DeployedLandingZone(UUID id, List<LandingZoneResource> deployedResources) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UUID id;
    private List<LandingZoneResource> deployedResources;

    public Builder id(UUID id) {
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
