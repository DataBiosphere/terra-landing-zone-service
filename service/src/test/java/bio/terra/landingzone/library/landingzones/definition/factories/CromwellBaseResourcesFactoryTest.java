package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import bio.terra.landingzone.library.landingzones.LandingZoneTestFixture;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class CromwellBaseResourcesFactoryTest extends LandingZoneTestFixture {

  private LandingZoneManager landingZoneManager;

  @BeforeAll
  static void setUpForAll() {
    Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
  }

  @BeforeEach
  void setUp() {
    landingZoneManager =
        LandingZoneManager.createLandingZoneManager(
            tokenCredential, azureProfile, resourceGroup.name());
  }

  @Test
  void deploysLandingZoneV1_resourcesAreCreated() throws InterruptedException {
    String landingZoneId = UUID.randomUUID().toString();
    var resources =
        landingZoneManager
            .deployLandingZoneAsync(
                landingZoneId,
                CromwellBaseResourcesFactory.class.getSimpleName(),
                DefinitionVersion.V1,
                null)
            .collectList()
            .block();

    // Note that this resource list does not include pre-requisite resources
    assertThat(resources, hasSize(5));

    // check if you can read lz resources
    TimeUnit.SECONDS.sleep(3); // wait for tag propagation...
    var sharedResources = landingZoneManager.reader().listSharedResources(landingZoneId);
    assertThat(sharedResources, hasSize(5));

    assertHasVnetWithPurpose(landingZoneId, SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);
    assertHasVnetWithPurpose(landingZoneId, SubnetResourcePurpose.AKS_NODE_POOL_SUBNET);
    assertHasVnetWithPurpose(landingZoneId, SubnetResourcePurpose.WORKSPACE_BATCH_SUBNET);
    assertHasVnetWithPurpose(landingZoneId, SubnetResourcePurpose.POSTGRESQL_SUBNET);
  }

  @Test
  void deleteLandingZoneResources_resourcesAreDeleted()
      throws InterruptedException, LandingZoneRuleDeleteException {
    String landingZoneId = UUID.randomUUID().toString();
    var resources =
        landingZoneManager
            .deployLandingZoneAsync(
                landingZoneId,
                CromwellBaseResourcesFactory.class.getSimpleName(),
                DefinitionVersion.V1,
                null)
            .collectList()
            .block();

    var deletedResources = landingZoneManager.deleteResources(landingZoneId);

    // Immediate listing after deletion may return transient resources results.
    await()
        .atMost(Duration.ofSeconds(60))
        .until(
            () ->
                armManagers
                        .azureResourceManager()
                        .genericResources()
                        .listByResourceGroup(resourceGroup.name())
                        .stream()
                        .count()
                    == 0);
  }

  private void assertHasVnetWithPurpose(String landingZoneId, SubnetResourcePurpose purpose) {
    var vNet = landingZoneManager.reader().listVNetBySubnetPurpose(landingZoneId, purpose);
    assertThat(vNet, hasSize(1));
  }
}
