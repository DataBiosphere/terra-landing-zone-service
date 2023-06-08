package bio.terra.landingzone.stairway.flight.create;

import bio.terra.landingzone.library.landingzones.TestArmResourcesFactory;
import org.junit.jupiter.api.Test;

public class WorkloadIdentityTest extends BaseIntegrationTest {
  @Test
  void testWorkloadIdentity() {
    resourceGroup =
        TestArmResourcesFactory.createTestResourceGroup(armManagers.azureResourceManager());
  }
}
