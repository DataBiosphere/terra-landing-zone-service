package bio.terra.landingzone.library.landingzones.management;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import bio.terra.landingzone.library.landingzones.AzureIntegrationUtils;
import bio.terra.landingzone.library.landingzones.TestArmResourcesFactory;
import bio.terra.landingzone.library.landingzones.TestUtils;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.definition.factories.TestLandingZoneFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Tag("integration")
class LandingZoneManagerIntegrationTest {

  private static AzureResourceManager azureResourceManager;
  private ResourceGroup resourceGroup;
  private LandingZoneManager landingZoneManager;

  @BeforeAll
  static void setUpTestResourceGroup() {
    Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
    azureResourceManager = TestArmResourcesFactory.createArmClient();
  }

  @BeforeEach
  void setUp() {
    resourceGroup = TestArmResourcesFactory.createTestResourceGroup(azureResourceManager);
    landingZoneManager =
        LandingZoneManager.createLandingZoneManager(
            AzureIntegrationUtils.getAzureCredentialsOrDie(),
            AzureIntegrationUtils.TERRA_DEV_AZURE_PROFILE,
            resourceGroup.name(),
            null /*ignore this value in test*/);
  }

  @AfterEach
  void cleanUp() {
    azureResourceManager.resourceGroups().deleteByName(resourceGroup.name());
  }

  @Test
  void deployLandingZone_deploysTestLandingZoneDefinition() {
    List<DeployedResource> resources =
        landingZoneManager.deployLandingZone(
            UUID.randomUUID().toString(),
            TestLandingZoneFactory.class.getSimpleName(),
            DefinitionVersion.V1,
            null);

    // the test landing zone creates two resources: storage account and vnet.
    assertThat(resources, hasSize(2));
    MatcherAssert.assertThat(TestUtils.findFirstStorageAccountId(resources), is(notNullValue()));
    assertThat(TestUtils.findFirstVNetId(resources), is(notNullValue()));
  }

  @Test
  void deployLandingZone_duplicateDeploymentWithRetry_deploysSuccessfullyOnlyOneInstance() {
    String landingZone = UUID.randomUUID().toString();
    Flux<DeployedResource> first =
        landingZoneManager
            .deployLandingZoneAsync(
                landingZone,
                TestLandingZoneFactory.class.getSimpleName(),
                DefinitionVersion.V1,
                null)
            .retryWhen(Retry.max(1));

    Flux<DeployedResource> second =
        landingZoneManager
            .deployLandingZoneAsync(
                landingZone,
                TestLandingZoneFactory.class.getSimpleName(),
                DefinitionVersion.V1,
                null)
            .retryWhen(Retry.max(1));

    var results = Flux.merge(first, second).collectList().block();

    // There should be more than 2 items in the list.
    assertThat(results, hasSize(greaterThan(2)));

    // however there should be only 2 distinct resources.
    var distinct = results.stream().distinct().collect(Collectors.toList());
    assertThat(distinct, hasSize(2));
    assertThat(TestUtils.findFirstStorageAccountId(distinct), is(notNullValue()));
    assertThat(TestUtils.findFirstVNetId(distinct), is(notNullValue()));

    assertThatExpectedResourcesExistsInResourceGroup(distinct);
  }

  @Test
  void deleteResources_deletesLandingZoneResources() throws LandingZoneRuleDeleteException {
    String landingZone = UUID.randomUUID().toString();
    List<DeployedResource> lz =
        landingZoneManager.deployLandingZone(
            landingZone, TestLandingZoneFactory.class.getSimpleName(), DefinitionVersion.V1, null);

    // look for resources in the RG directly to confirm tags are indexed.
    await()
        .atMost(Duration.ofSeconds(60))
        .until(
            () -> {
              return landingZoneManager.reader().listAllResources(landingZone).size() == 2;
            });

    var deleted = landingZoneManager.deleteResources(landingZone);

    assertThat(deleted, hasSize(2));
    await()
        .atMost(Duration.ofSeconds(60))
        .until(
            () ->
                azureResourceManager
                        .genericResources()
                        .listByResourceGroup(resourceGroup.name())
                        .stream()
                        .count()
                    == 0);
  }

  private void assertThatExpectedResourcesExistsInResourceGroup(List<DeployedResource> result) {

    var resourcesInGroup =
        azureResourceManager.genericResources().listByResourceGroup(resourceGroup.name()).stream()
            .collect(Collectors.toList());

    assertThat(resourcesInGroup, hasSize(2));
    assertThat(
        resourcesInGroup.stream()
            .filter(r -> r.id().equals(TestUtils.findFirstStorageAccountId(result)))
            .findFirst()
            .get(),
        is(notNullValue()));
    assertThat(
        resourcesInGroup.stream()
            .filter(r -> r.id().equals(TestUtils.findFirstVNetId(result)))
            .findFirst()
            .get(),
        is(notNullValue()));
  }

  @Test
  void listDefinitionFactories_testFactoryIsListed() {
    var factories = LandingZoneManager.listDefinitionFactories();
    FactoryDefinitionInfo testFactory =
        new FactoryDefinitionInfo(
            TestLandingZoneFactory.LZ_NAME,
            TestLandingZoneFactory.LZ_DESC,
            TestLandingZoneFactory.class.getSimpleName(),
            List.of(DefinitionVersion.V1));

    assertThat(factories, hasItem(testFactory));
  }
}
