package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import bio.terra.landingzone.library.landingzones.LandingZoneTestFixture;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import com.azure.resourcemanager.resources.models.GenericResource;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;
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
    landingZoneManager
        .deployLandingZoneAsync(
            landingZoneId,
            CromwellBaseResourcesFactory.class.getSimpleName(),
            DefinitionVersion.V1,
            null)
        .collectList()
        .block();

    // check if you can read lz resources
    await() // wait for tag propagation...
        .atMost(Duration.ofSeconds(120))
        .untilAsserted(
            () -> {
              var landingZoneResources =
                  landingZoneManager.reader().listAllResources(landingZoneId);
              assertThat(landingZoneResources, hasSize(9));
            });

    assertThat(landingZoneManager.reader().listSharedResources(landingZoneId), hasSize(8));
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
        .atMost(Duration.ofSeconds(120))
        .until(
            () -> {
              Stream<GenericResource> stream =
                  armManagers
                      .azureResourceManager()
                      .genericResources()
                      .listByResourceGroup(resourceGroup.name())
                      .stream();
              return stream.count() == 0;
            });
  }

  private void assertHasVnetWithPurpose(String landingZoneId, SubnetResourcePurpose purpose) {
    var vNet = landingZoneManager.reader().listVNetBySubnetPurpose(landingZoneId, purpose);
    assertThat(vNet, hasSize(1));
  }
}
