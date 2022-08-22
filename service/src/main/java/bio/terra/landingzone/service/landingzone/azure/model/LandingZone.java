package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LandingZone {
  private final String id;
  private final List<LandingZoneResource> deployedResources;

  public LandingZone(String id, List<LandingZoneResource> deployedResources) {
    this.id = id;
    this.deployedResources = deployedResources;
  }

  public String getId() {
    return id;
  }

  public List<LandingZoneResource> getDeployedResources() {
    return deployedResources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    LandingZone landingZone = (LandingZone) o;

    return new EqualsBuilder()
        .append(id, landingZone.id)
        .append(deployedResources, landingZone.deployedResources)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(id).append(deployedResources).toHashCode();
  }

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

    public LandingZone build() {
      return new LandingZone(id, deployedResources);
    }
  }
}
