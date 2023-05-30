package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.library.landingzones.AzureIntegrationUtils;
import bio.terra.landingzone.library.landingzones.TestArmResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import org.junit.jupiter.api.BeforeAll;

public class BaseIntegrationTest {
  protected static ArmManagers armManagers;
  protected static ResourceGroup resourceGroup;

  protected static TokenCredential tokenCredential;
  protected static AzureProfile azureProfile;

  @BeforeAll
  static void setUpBeforeAll() {
    armManagers =
        new ArmManagers(
            TestArmResourcesFactory.createArmClient(),
            TestArmResourcesFactory.createRelayArmClient(),
            TestArmResourcesFactory.createBatchArmClient(),
            TestArmResourcesFactory.createPostgreSqlArmClient(),
            TestArmResourcesFactory.createLogAnalyticsArmClient(),
            TestArmResourcesFactory.createMonitorArmClient(),
            TestArmResourcesFactory.createApplicationInsightsArmClient(),
            TestArmResourcesFactory.createSecurityInsightsArmClient());
    tokenCredential = AzureIntegrationUtils.getAdminAzureCredentialsOrDie();
    azureProfile = AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE;
  }
}
