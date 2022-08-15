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
class ManagedNetworkWithSharedResourcesFactoryTest extends LandingZoneTestFixture {

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
                ManagedNetworkWithSharedResourcesFactory.class.getName(),
                DefinitionVersion.V1,
                null)
            .collectList()
            .block();

    assertThat(resources, hasSize(4));

    // check if you can read lz resources
    TimeUnit.SECONDS.sleep(3); // wait for tag propagation...
    var sharedResources = landingZoneManager.reader().listSharedResources();
    assertThat(sharedResources, hasSize(3));
    var vNet =
        landingZoneManager
            .reader()
            .listVNetWithSubnetPurpose(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);
    assertThat(vNet, hasSize(1));
  }
}
