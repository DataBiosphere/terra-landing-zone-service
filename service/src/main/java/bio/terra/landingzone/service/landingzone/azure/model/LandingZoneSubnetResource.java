package bio.terra.landingzone.service.landingzone.azure.model;

public record LandingZoneSubnetResource(
    String name, String subnetPurpose, String vNetId, String region) {

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
