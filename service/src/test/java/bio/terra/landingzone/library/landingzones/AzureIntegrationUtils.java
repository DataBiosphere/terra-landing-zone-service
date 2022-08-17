package bio.terra.landingzone.library.landingzones;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.assertj.core.util.Strings;

public class AzureIntegrationUtils {
  /** Path to Azure properties file. */
  private static final String AZURE_PROPERTIES_PATH = "integration_azure_env.properties";

  /** Property prefix for properties in {@link #AZURE_PROPERTIES_PATH}. */
  private static final String AZURE_PROPERTY_PREFIX = "integration.azure";

  private static final String CLIENT_ID_ENV_VAR = "AZURE_PUBLISHER_CLIENT_ID";
  private static final String CLIENT_SECRET_ENV_VAR = "AZURE_PUBLISHER_CLIENT_SECRET";
  private static final String TENANT_ID_ENV_VAR = "AZURE_PUBLISHER_TENANT_ID";

  // 8201558-terra-dev
  public static final AzureProfile TERRA_DEV_AZURE_PROFILE =
      new AzureProfile(
          "0cb7a640-45a2-4ed6-be9f-63519f86e04b",
          "c5f8eca3-f512-48cb-b01f-f19f1af9014c",
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
    return getClientSecretCredentialFromPropertiesFile();
  }

  private static ClientSecretCredential getClientSecretCredentialFromPropertiesFile() {
    try (InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(AZURE_PROPERTIES_PATH)) {
      Properties properties = new Properties();
      properties.load(in);

      final String clientId =
          Preconditions.checkNotNull(
              properties.getProperty(AZURE_PROPERTY_PREFIX + ".admin.clientId"),
              "Unable to read Azure admin client id from " + AZURE_PROPERTIES_PATH);

      final String clientSecret =
          Preconditions.checkNotNull(
              properties.getProperty(AZURE_PROPERTY_PREFIX + ".admin.clientSecret"),
              "Unable to read Azure admin application secret from " + AZURE_PROPERTIES_PATH);

      final String tenantId =
          Preconditions.checkNotNull(
              properties.getProperty(AZURE_PROPERTY_PREFIX + ".admin.tenantId"),
              "Unable to read Azure admin tenant id from " + AZURE_PROPERTIES_PATH);

      return new ClientSecretCredentialBuilder()
          .clientId(clientId)
          .clientSecret(clientSecret)
          .tenantId(tenantId)
          .build();

    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to load Azure properties file from " + AZURE_PROPERTIES_PATH, e);
    }
  }

  public static TokenCredential getAdminCredentialsFromEnvironmentVariables() {
    final String clientId = System.getenv(CLIENT_ID_ENV_VAR);
    final String clientSecret = System.getenv(CLIENT_SECRET_ENV_VAR);
    final String tenantId = System.getenv(TENANT_ID_ENV_VAR);

    if (Strings.isNullOrEmpty(clientId)
        || Strings.isNullOrEmpty(clientSecret)
        || Strings.isNullOrEmpty(tenantId)) {
      return null;
    }

    return new ClientSecretCredentialBuilder()
        .clientId(clientId)
        .clientSecret(clientSecret)
        .tenantId(tenantId)
        .build();
  }
}
