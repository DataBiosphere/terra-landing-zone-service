package bio.terra.landingzone.library.configuration;

public class LandingZoneAzureConfiguration {
  // Managed app authentication
  private String managedAppClientId;
  private String managedAppClientSecret;
  private String managedAppTenantId;
  private Long sasTokenStartTimeMinutesOffset;
  private Long sasTokenExpiryTimeMinutesOffset;

  public String getManagedAppClientId() {
    return managedAppClientId;
  }

  public void setManagedAppClientId(String managedAppClientId) {
    this.managedAppClientId = managedAppClientId;
  }

  public String getManagedAppClientSecret() {
    return managedAppClientSecret;
  }

  public void setManagedAppClientSecret(String managedAppClientSecret) {
    this.managedAppClientSecret = managedAppClientSecret;
  }

  public String getManagedAppTenantId() {
    return managedAppTenantId;
  }

  public void setManagedAppTenantId(String managedAppTenantId) {
    this.managedAppTenantId = managedAppTenantId;
  }

  public Long getSasTokenStartTimeMinutesOffset() {
    return sasTokenStartTimeMinutesOffset;
  }

  public void setSasTokenStartTimeMinutesOffset(Long sasTokenStartTimeMinutesOffset) {
    this.sasTokenStartTimeMinutesOffset = sasTokenStartTimeMinutesOffset;
  }

  public Long getSasTokenExpiryTimeMinutesOffset() {
    return sasTokenExpiryTimeMinutesOffset;
  }

  public void setSasTokenExpiryTimeMinutesOffset(Long sasTokenExpiryTimeMinutesOffset) {
    this.sasTokenExpiryTimeMinutesOffset = sasTokenExpiryTimeMinutesOffset;
  }
}
