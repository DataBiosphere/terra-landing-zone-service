package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.landingzone.library.landingzones.LandingZoneTestFixture;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.factories.exception.InvalidInputParameterException;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

@Tag("integration")
@Disabled("we don't create landing zones this way anymore")
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
  void deploysLandingZoneV1_resourcesAreCreatedAndDeleted()
      throws InterruptedException, LandingZoneRuleDeleteException {
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

    landingZoneManager.deleteResources(landingZoneId);

    // Immediate listing after deletion may return transient resources results.
    await()
        .atMost(Duration.ofSeconds(120))
        .untilAsserted(
            () -> {
              var landingZoneResources =
                  landingZoneManager.reader().listAllResources(landingZoneId);
              assertThat(landingZoneResources, empty());
            });
  }

  @Test
  void deployLandingZoneV1_CorsMaxAgeValidationFailed() {
    var invalidCorsMaxAge =
        Map.of(
            StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name(), "-10");

    Exception e =
        assertThrows(
            InvalidInputParameterException.class,
            () ->
                landingZoneManager
                    .deployLandingZoneAsync(
                        UUID.randomUUID().toString(),
                        CromwellBaseResourcesFactory.class.getSimpleName(),
                        DefinitionVersion.V1,
                        invalidCorsMaxAge)
                    .collectList()
                    .block());
    assertTrue(
        e.getMessage()
            .contains(
                StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name()));
  }

  private void assertHasVnetWithPurpose(String landingZoneId, SubnetResourcePurpose purpose) {
    var vNet = landingZoneManager.reader().listVNetBySubnetPurpose(landingZoneId, purpose);
    assertThat(vNet, hasSize(1));
  }
}
