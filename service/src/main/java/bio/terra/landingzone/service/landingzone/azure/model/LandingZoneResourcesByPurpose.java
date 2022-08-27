package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.List;
import java.util.Map;

public record LandingZoneResourcesByPurpose(
    Map<String, List<LandingZoneResource>> generalResources,
    Map<String, List<LandingZoneSubnetResource>> subnetResources) {

  public static Builder builder() {
    return new Builder();
  }

  public Map<String, List<LandingZoneResource>> getGeneralResources() {
    return generalResources;
  }

  public Map<String, List<LandingZoneSubnetResource>> getSubnetResources() {
    return subnetResources;
  }

  public static class Builder {
    private Map<String, List<LandingZoneResource>> generalResources;
    private Map<String, List<LandingZoneSubnetResource>> subnetResources;

    public Builder generalResources(Map<String, List<LandingZoneResource>> generalResources) {
      this.generalResources = generalResources;
      return this;
    }

    public Builder subnetResources(Map<String, List<LandingZoneSubnetResource>> subnetResources) {
      this.subnetResources = subnetResources;
      return this;
    }

    public LandingZoneResourcesByPurpose build() {
      return new LandingZoneResourcesByPurpose(generalResources, subnetResources);
    }
  }
}
