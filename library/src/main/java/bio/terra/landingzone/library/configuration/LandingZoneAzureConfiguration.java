package bio.terra.landingzone.library.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "workspace.azure")
public class LandingZoneAzureConfiguration {
  // Managed app authentication
  private String managedAppClientId;
  private String managedAppClientSecret;
  private String managedAppTenantId;

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
}
