package bio.terra.landingzone.service.landingzone.azure.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LandingZoneSubnetResource {
  private final String name;
  private final String subnetPurpose;

  // VNet information used for flattened subnet view.
  private final String vNetId;
  private final String region;

  public LandingZoneSubnetResource(
      String name, String subnetPurpose, String vNetId, String region) {
    this.name = name;
    this.subnetPurpose = subnetPurpose;
    this.vNetId = vNetId;
    this.region = region;
  }

  public String getName() {
    return name;
  }

  public String getSubnetPurpose() {
    return subnetPurpose;
  }

  public String getVNetId() {
    return vNetId;
  }

  public String getRegion() {
    return region;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    LandingZoneSubnetResource subnetResource = (LandingZoneSubnetResource) o;

    return new EqualsBuilder()
        .append(name, subnetResource.name)
        .append(subnetPurpose, subnetResource.subnetPurpose)
        .append(vNetId, subnetResource.vNetId)
        .append(region, subnetResource.region)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(name)
        .append(subnetPurpose)
        .append(vNetId)
        .append(region)
        .toHashCode();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String subnetPurpose;
    private String vNetId;
    private String region;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder subnetPurpose(String subnetPurpose) {
      this.subnetPurpose = subnetPurpose;
      return this;
    }

    public Builder vNetId(String vNetId) {
      this.vNetId = vNetId;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public LandingZoneSubnetResource build() {
      return new LandingZoneSubnetResource(name, subnetPurpose, vNetId, region);
    }
  }
}
