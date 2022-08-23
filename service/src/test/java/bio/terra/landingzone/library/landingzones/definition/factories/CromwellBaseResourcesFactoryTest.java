package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import bio.terra.landingzone.library.landingzones.LandingZoneTestFixture;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class CromwellBaseResourcesFactoryTest extends LandingZoneTestFixture {

  private LandingZoneManager landingZoneManager;

  @BeforeEach
  void setUp() {
    landingZoneManager =
        LandingZoneManager.createLandingZoneManager(
            tokenCredential, azureProfile, resourceGroup.name());
  }

  @Test
  void deploysLandingZoneV1_resourcesAreCreated() throws InterruptedException {
    var resources =
        landingZoneManager
            .deployLandingZoneAsync(
                UUID.randomUUID().toString(),
                CromwellBaseResourcesFactory.class.getSimpleName(),
                DefinitionVersion.V1,
                null)
            .collectList()
            .block();

    // Note that this resource list does not include pre-requisite resources
    assertThat(resources, hasSize(5));

    // check if you can read lz resources
    TimeUnit.SECONDS.sleep(3); // wait for tag propagation...
    var sharedResources = landingZoneManager.reader().listSharedResources();
    assertThat(sharedResources, hasSize(5));

    assertHasVnetWithPurpose(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);
    assertHasVnetWithPurpose(SubnetResourcePurpose.AKS_NODE_POOL_SUBNET);
    assertHasVnetWithPurpose(SubnetResourcePurpose.WORKSPACE_BATCH_SUBNET);
    assertHasVnetWithPurpose(SubnetResourcePurpose.POSTGRESQL_SUBNET);
  }

  private void assertHasVnetWithPurpose(SubnetResourcePurpose purpose) {
    var vNet = landingZoneManager.reader().listVNetWithSubnetPurpose(purpose);
    assertThat(vNet, hasSize(1));
  }
}
