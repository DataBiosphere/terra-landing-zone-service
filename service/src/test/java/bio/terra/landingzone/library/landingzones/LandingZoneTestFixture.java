package bio.terra.landingzone.library.landingzones;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/** wires up ARM clients and creates a test resource group. */
public class LandingZoneTestFixture {
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
            TestArmResourcesFactory.createPostgreSqlArmClient());
    resourceGroup =
        TestArmResourcesFactory.createTestResourceGroup(armManagers.azureResourceManager());
    tokenCredential = AzureIntegrationUtils.getAdminAzureCredentialsOrDie();
    azureProfile = AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE;
  }

  @AfterAll
  static void cleanUpAfterAll() {
    armManagers.azureResourceManager().resourceGroups().deleteByName(resourceGroup.name());
  }
}
