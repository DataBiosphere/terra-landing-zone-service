package bio.terra.landingzone.library.landingzones;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;

public class AzureIntegrationUtils {

  /** Path to Azure properties file. */
  private static final String AZURE_PROPERTIES_PATH = "integration_azure_env.properties";

  /** Property prefix for properties in {@link #AZURE_PROPERTIES_PATH}. */
  private static final String AZURE_PROPERTY_PREFIX = "workspace.azure.";

  // 8201558-dsp-azure-testing
  public static final AzureProfile TERRA_DEV_AZURE_PROFILE =
      new AzureProfile(
          "fad90753-2022-4456-9b0a-c7e5b934e408",
          "f557c728-871d-408c-a28b-eb6b2141a087",
          AzureEnvironment.AZURE);

  /**
   * Gets an Azure TokenCredential object for an Azure admin account. This account has the roles
   * needed to operate the integration test project, e.g. create and delete resources.
   *
   * @return TokenCredential
   */
  public static TokenCredential getAdminAzureCredentialsOrDie() {
    TokenCredential credential = getAdminCredentialsFromEnvironmentVariables();
    if (credential != null) {
      return credential;
    }
    throw new RuntimeException("Not supported");
  }

  public static TokenCredential getAdminCredentialsFromEnvironmentVariables() {
    return new DefaultAzureCredentialBuilder().build();
  }
}
