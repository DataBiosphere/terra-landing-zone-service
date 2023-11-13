package bio.terra.landingzone.library.landingzones;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.relay.RelayManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.securityinsights.SecurityInsightsManager;
import java.util.Locale;
import java.util.UUID;

public class TestArmResourcesFactory {

  public static AzureResourceManager createArmClient() {
    AzureProfile profile = AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE;
    return AzureResourceManager.authenticate(
            AzureIntegrationUtils.getAzureCredentialsOrDie(), profile)
        .withSubscription(profile.getSubscriptionId());
  }

  public static ArmManagers createArmManagers() {
    return new ArmManagers(
        createArmClient(),
        createRelayArmClient(),
        createBatchArmClient(),
        createPostgreSqlArmClient(),
        createLogAnalyticsArmClient(),
        createMonitorArmClient(),
        createApplicationInsightsArmClient(),
        createSecurityInsightsArmClient());
  }

  public static RelayManager createRelayArmClient() {
    return RelayManager.authenticate(
        AzureIntegrationUtils.getAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static BatchManager createBatchArmClient() {
    return BatchManager.authenticate(
        AzureIntegrationUtils.getAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static PostgreSqlManager createPostgreSqlArmClient() {
    return PostgreSqlManager.authenticate(
        AzureIntegrationUtils.getAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static LogAnalyticsManager createLogAnalyticsArmClient() {
    return LogAnalyticsManager.authenticate(
        AzureIntegrationUtils.getAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static MonitorManager createMonitorArmClient() {
    return MonitorManager.authenticate(
        AzureIntegrationUtils.getAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static ApplicationInsightsManager createApplicationInsightsArmClient() {
    return ApplicationInsightsManager.authenticate(
        AzureIntegrationUtils.getAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static SecurityInsightsManager createSecurityInsightsArmClient() {
    return SecurityInsightsManager.authenticate(
        AzureIntegrationUtils.getAzureCredentialsOrDie(),
        AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE);
  }

  public static ResourceGroup createTestResourceGroup(AzureResourceManager azureResourceManager) {
    String resourceGroupId = UUID.randomUUID().toString().replace("-", "");
    return azureResourceManager
        .resourceGroups()
        .define("rg" + resourceGroupId)
        .withRegion(Region.US_SOUTH_CENTRAL)
        .withTag("PURPOSE", "LANDING_ZONE_SERVICE_INTEGRATION_TESTING")
        .create();
  }

  public static String createUniqueAzureResourceName() {
    return UUID.randomUUID().toString().toLowerCase(Locale.ROOT).replace("-", "").substring(0, 23);
  }
}
