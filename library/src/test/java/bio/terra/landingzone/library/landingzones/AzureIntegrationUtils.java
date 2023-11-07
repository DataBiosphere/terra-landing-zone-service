package bio.terra.landingzone.library.landingzones;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;

public class AzureIntegrationUtils {

  // 8201558-dsp-azure-testing
  public static final AzureProfile TERRA_DEV_AZURE_PROFILE =
      new AzureProfile(
          "fad90753-2022-4456-9b0a-c7e5b934e408",
          "f557c728-871d-408c-a28b-eb6b2141a087",
          AzureEnvironment.AZURE);

  /**
   * Gets an Azure TokenCredential object for an Azure integration testing account. This account has
   * the roles needed to operate the integration test project, e.g. create and delete resources in a
   * test resource group.
   *
   * @return TokenCredential
   */
  public static TokenCredential getAzureCredentialsOrDie() {
    TokenCredential credential = new DefaultAzureCredentialBuilder().build();
    if (credential != null) {
      return credential;
    }
    throw new RuntimeException("Not supported");
  }
}
