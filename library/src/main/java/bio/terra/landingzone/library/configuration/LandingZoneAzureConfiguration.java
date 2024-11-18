package bio.terra.landingzone.library.configuration;

import com.azure.core.management.AzureEnvironment;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "workspace.azure")
public class LandingZoneAzureConfiguration {
  // Managed app authentication
  private String managedAppClientId;
  private String managedAppClientSecret;
  private String managedAppTenantId;
  private String azureEnvironment;

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


  // AzureCloud or AzureUSGovernmentCloud
  public AzureEnvironment getAzureEnvironment() {
    switch (azureEnvironment) {
      case "AzureCloud":
        return AzureEnvironment.AZURE;
      case "AzureUSGovernmentCloud":
        return AzureEnvironment.AZURE_US_GOVERNMENT;
      default:
        throw new IllegalArgumentException(
            String.format("Unknown Azure environment: %s", azureEnvironment));
    }
  }

  public void setAzureEnvironment(String azureEnvironment) {
    this.azureEnvironment = azureEnvironment;
  }
}
