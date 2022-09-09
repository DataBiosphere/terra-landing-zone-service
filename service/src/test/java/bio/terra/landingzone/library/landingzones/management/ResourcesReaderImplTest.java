package bio.terra.landingzone.library.landingzones.management;

import static org.awaitility.Awaitility.await;

import bio.terra.landingzone.library.landingzones.TestArmResourcesFactory;
import bio.terra.landingzone.library.landingzones.TestUtils;
import bio.terra.landingzone.library.landingzones.definition.DefinitionContext;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionProvider;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionProviderImpl;
import bio.terra.landingzone.library.landingzones.definition.factories.TestLandingZoneFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.DeployedVNet;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment.DefinitionStages.WithLandingZoneResource;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployments;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeploymentsImpl;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class ResourcesReaderImplTest {

  private static String landingZoneId;
  private static AzureResourceManager azureResourceManager;
  private static ResourceGroup resourceGroup;
  private static LandingZoneDefinitionFactory landingZoneFactory;

  private static LandingZoneDefinitionProvider landingZoneDefinitionProvider;

  private static LandingZoneDeployments landingZoneDeployments;

  private static WithLandingZoneResource landingZoneResourceDeployment;

  private static List<DeployedResource> landingZoneResources;
  private static DeployedResource deployedStorage;
  private static DeployedVNet deployedVNet;
  private ResourcesReader resourcesReader;

  @BeforeAll
  static void setUpTestLandingZone() throws InterruptedException {
    azureResourceManager = TestArmResourcesFactory.createArmClient();
    resourceGroup = TestArmResourcesFactory.createTestResourceGroup(azureResourceManager);
    landingZoneDeployments = new LandingZoneDeploymentsImpl();
    landingZoneId = UUID.randomUUID().toString();
    landingZoneResourceDeployment = landingZoneDeployments.define(landingZoneId);

    landingZoneDefinitionProvider =
        new LandingZoneDefinitionProviderImpl(TestArmResourcesFactory.createArmManagers());
    landingZoneFactory =
        landingZoneDefinitionProvider.createDefinitionFactory(TestLandingZoneFactory.class);
    landingZoneResources =
        landingZoneFactory
            .create(DefinitionVersion.V1)
            .definition(
                new DefinitionContext(
                    landingZoneId,
                    landingZoneResourceDeployment,
                    resourceGroup,
                    new ResourceNameGenerator(landingZoneId),
                    new HashMap<>()))
            .deploy();
    deployedStorage = getDeployedStorage();
    deployedVNet = getDeployedVNet();
    TimeUnit.SECONDS.sleep(10); // give some time for replication of tag data
    Awaitility.setDefaultPollInterval(Duration.ofSeconds(1));
  }

  @AfterAll
  static void cleanUpArmResources() {
    azureResourceManager.resourceGroups().deleteByName(resourceGroup.name());
  }

  private static DeployedResource getDeployedStorage() {
    return landingZoneResources.stream()
        .filter(c -> c.resourceType().equalsIgnoreCase("Microsoft.Storage/storageAccounts"))
        .findFirst()
        .get();
  }

  private static DeployedVNet getDeployedVNet() {
    var deployedVNet =
        landingZoneResources.stream()
            .filter(c -> c.resourceType().equalsIgnoreCase("Microsoft.Network/virtualNetworks"))
            .findFirst()
            .get();

    var vNet = azureResourceManager.networks().getById(deployedVNet.resourceId());

    HashMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap = new HashMap<>();

    SubnetResourcePurpose.values()
        .forEach(
            p -> {
              var subnetName = vNet.tags().get(p.toString());
              if (subnetName != null) {
                var subnet = vNet.subnets().get(subnetName);
                subnetHashMap.put(
                    p,
                    new DeployedSubnet(subnet.id(), subnet.name(), vNet.id(), vNet.regionName()));
              }
            });

    return new DeployedVNet(vNet.id(), subnetHashMap, vNet.regionName());
  }

  @BeforeEach
  void setUp() {
    resourcesReader = new ResourcesReaderImpl(azureResourceManager, resourceGroup);
  }

  @Test
  void listSharedResources_storageResourceIsSharedResource() throws InterruptedException {

    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              var resources = resourcesReader.listSharedResources(landingZoneId);
              return resources.size() == 1
                  && deployedStorage
                      .resourceId()
                      .equalsIgnoreCase(TestUtils.findFirstStorageAccountId(resources));
            });
  }

  @Test
  void listResourcesByPurpose_storageResourceIsSharedResource() throws InterruptedException {

    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              var resources =
                  resourcesReader.listResourcesByPurpose(ResourcePurpose.SHARED_RESOURCE);
              return resources.size() == 1
                  && deployedStorage
                      .resourceId()
                      .equalsIgnoreCase(TestUtils.findFirstStorageAccountId(resources));
            });
  }

  @Test
  void listResourcesWithPurpose() throws InterruptedException {
    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              var resources = resourcesReader.listResources();
              return resources.size() == 1
                  && resources
                      .get(0)
                      .tags()
                      .containsKey(String.valueOf(LandingZoneTagKeys.LANDING_ZONE_PURPOSE))
                  && resources
                          .get(0)
                          .tags()
                          .get(String.valueOf(LandingZoneTagKeys.LANDING_ZONE_PURPOSE))
                      != null;
            });
  }

  @Test
  void listSubnetsWithSubnetPurpose() throws InterruptedException {
    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              var resources =
                  resourcesReader.listSubnetsWithSubnetPurpose(
                      SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);
              return resources.size() == 1 && deployedVNet.Id().equals(resources.get(0).vNetId());
            });
  }

  @Test
  void listVNetWithSubnetPurpose_returnsDeployedVNet() throws InterruptedException {

    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              var resources =
                  resourcesReader.listVNetWithSubnetPurpose(
                      SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);
              return resources.size() == 1
                  && deployedVNet.Id().equalsIgnoreCase(resources.iterator().next().Id());
            });
  }
}
