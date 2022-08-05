package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class AzureLandingZone {
  private final String id;
  private final List<AzureLandingZoneResource> deployedResources;

  public AzureLandingZone(String id, List<AzureLandingZoneResource> deployedResources) {
    this.id = id;
    this.deployedResources = deployedResources;
  }

  public String getId() {
    return id;
  }

  public List<AzureLandingZoneResource> getDeployedResources() {
    return deployedResources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    AzureLandingZone azureLandingZone = (AzureLandingZone) o;

    return new EqualsBuilder()
        .append(id, azureLandingZone.id)
        .append(deployedResources, azureLandingZone.deployedResources)
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
    private List<AzureLandingZoneResource> deployedResources;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder deployedResources(List<AzureLandingZoneResource> deployedResources) {
      this.deployedResources = deployedResources;
      return this;
    }

    public AzureLandingZone build() {
      return new AzureLandingZone(id, deployedResources);
    }
  }
}
