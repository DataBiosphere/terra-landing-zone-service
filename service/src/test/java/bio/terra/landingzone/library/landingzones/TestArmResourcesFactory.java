package bio.terra.landingzone.library.landingzones;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.relay.RelayManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.Locale;
import java.util.UUID;

public class TestArmResourcesFactory {

  public static AzureResourceManager createArmClient() {
    AzureProfile profile = AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE;
    return AzureResourceManager.authenticate(
            AzureIntegrationUtils.getAdminAzureCredentialsOrDie(), profile)
        .withSubscription(profile.getSubscriptionId());
  }

  public static ArmManagers createArmManagers() {
    return new ArmManagers(
        createArmClient(),
        createRelayArmClient(),
        createBatchArmClient(),
        createPostgreSqlArmClient());
  }

  public static RelayManager createRelayArmClient() {
    return RelayManager.authenticate(
        AzureIntegrationUtils.getAdminAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static BatchManager createBatchArmClient() {
    return BatchManager.authenticate(
        AzureIntegrationUtils.getAdminAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static PostgreSqlManager createPostgreSqlArmClient() {
    return PostgreSqlManager.authenticate(
        AzureIntegrationUtils.getAdminAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static ResourceGroup createTestResourceGroup(AzureResourceManager azureResourceManager) {
    String resourceGroupId = UUID.randomUUID().toString();
    return azureResourceManager
        .resourceGroups()
        .define("test-" + resourceGroupId)
        .withRegion(Region.US_WEST2)
        .create();
  }

  public static String createUniqueAzureResourceName() {
    return UUID.randomUUID().toString().toLowerCase(Locale.ROOT).replace("-", "").substring(0, 23);
  }
}
