package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import bio.terra.landingzone.library.landingzones.LandingZoneTestFixture;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import java.time.Duration;
import java.util.UUID;
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
  void deploysLandingZoneV1_resourcesAreCreated() {
    String landingZoneId = UUID.randomUUID().toString();
    var resources =
        landingZoneManager
            .deployLandingZoneAsync(
                landingZoneId,
                ManagedNetworkWithSharedResourcesFactory.class.getSimpleName(),
                DefinitionVersion.V1,
                null)
            .collectList()
            .block();

    // three resources deployed -vnet, relay and storage
    assertThat(resources, hasSize(3));

    // check if you can read lz resources
    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              var sharedResources = landingZoneManager.reader().listSharedResources(landingZoneId);
              return sharedResources.size()
                  == 2; // there should be two resources, relay and storage
            });

    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              var vNets =
                  landingZoneManager
                      .reader()
                      .listVNetWithSubnetPurpose(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);
              return vNets.size() == 1; // only one vnet.
            });
  }
}
