package bio.terra.landingzone.resource;

public enum ExternalResourceType {
  AZURE_LANDING_ZONE("AZURE");

  private final String cloudPlatform;

  ExternalResourceType(String cloudPlatform) {
    this.cloudPlatform = cloudPlatform;
  }

  public String getCloudPlatform() {
    return cloudPlatform;
  }
}
