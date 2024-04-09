package bio.terra.landingzone.library;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class will attempt to get Azure credentials, starting with our legacy spring configuration
 * and falling back to the `DefaultAzureCredentialsBuilder`.
 *
 * <p>It is intended as a backwards compatibility layer for WSM as it does not wire up the
 * credentials needed by DefaultAzureCredentialsBuilder in CI scenarios where landing zones are
 * involved.
 *
 * <p>This class should be removed upon full de-amalgamation from WSM.
 */
@Component
public class AzureCredentialsProvider {

  private final LandingZoneAzureConfiguration azureConfiguration;

  @Autowired
  public AzureCredentialsProvider(LandingZoneAzureConfiguration azureConfiguration) {
    this.azureConfiguration = azureConfiguration;
  }

  public TokenCredential getTokenCredential() {
    if (Objects.nonNull(azureConfiguration.getManagedAppTenantId())
        && Objects.nonNull(azureConfiguration.getManagedAppClientSecret())
        && Objects.nonNull(azureConfiguration.getManagedAppClientId())) {

      return new ClientSecretCredentialBuilder()
          .clientId(azureConfiguration.getManagedAppClientId())
          .clientSecret(azureConfiguration.getManagedAppClientSecret())
          .tenantId(azureConfiguration.getManagedAppTenantId())
          .build();
    }

    return new DefaultAzureCredentialBuilder().build();
  }
}
