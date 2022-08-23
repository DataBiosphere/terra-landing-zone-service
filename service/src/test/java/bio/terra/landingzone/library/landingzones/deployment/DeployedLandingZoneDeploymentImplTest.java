package bio.terra.landingzone.library.landingzones.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import bio.terra.landingzone.library.landingzones.TestArmResourcesFactory;
import bio.terra.landingzone.library.landingzones.TestUtils;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.PrivateLinkSubResourceName;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Tag("integration")
class DeployedLandingZoneDeploymentImplTest {

  private static AzureResourceManager azureResourceManager;
  private static ResourceGroup resourceGroup;
  private static String storageAccountName;
  private static String vNetName;
  private LandingZoneDeploymentsImpl landingZoneDeployments;

  private String landingZoneId;

  @BeforeAll
  static void setUpClients() {
    azureResourceManager = TestArmResourcesFactory.createArmClient();
    resourceGroup = TestArmResourcesFactory.createTestResourceGroup(azureResourceManager);
  }

  @AfterAll
  static void cleanUp() {
    azureResourceManager.resourceGroups().deleteByName(resourceGroup.name());
  }

  @BeforeEach
  void setUp() {
    landingZoneId = UUID.randomUUID().toString();
    landingZoneDeployments = new LandingZoneDeploymentsImpl();
    storageAccountName = TestArmResourcesFactory.createUniqueAzureResourceName();
    vNetName = TestArmResourcesFactory.createUniqueAzureResourceName();
  }

  @Test
  void deploy_sharedStorageAccount() {
    var storage =
        azureResourceManager
            .storageAccounts()
            .define(storageAccountName)
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup);

    var resources =
        landingZoneDeployments
            .define(landingZoneId)
            .withResourceWithPurpose(storage, ResourcePurpose.SHARED_RESOURCE)
            .deploy();

    assertThatSharedStorageAccountIsCreated(resources);
  }

  private void assertThatSharedStorageAccountIsCreated(List<DeployedResource> resources) {
    assertThat(resources, hasSize(1));
    DeployedResource resource = resources.iterator().next();

    var createdStorage = azureResourceManager.storageAccounts().getById(resource.resourceId());
    assertThat(createdStorage.id(), equalTo(resource.resourceId()));
    assertThat(resource.region(), equalTo(resourceGroup.regionName()));
    assertThat(
        resource.tags().get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()), equalTo(landingZoneId));
    assertThat(
        resource.tags().get(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()),
        equalTo(ResourcePurpose.SHARED_RESOURCE.toString()));
  }

  @Test
  void deploy_sharedVNetWithMultipleSubNets() {
    var vNet =
        azureResourceManager
            .networks()
            .define(vNetName)
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup)
            .withAddressSpace("10.0.0.0/28")
            .withSubnet("compute", "10.0.0.0/29")
            .withSubnet("storage", "10.0.0.8/29");

    var resources =
        landingZoneDeployments
            .define(landingZoneId)
            .withVNetWithPurpose(vNet, "compute", SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET)
            .withVNetWithPurpose(vNet, "storage", SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET)
            .deploy();

    assertThatVNetWithSubnetsIsCreated(resources);
  }

  private void assertThatVNetWithSubnetsIsCreated(List<DeployedResource> resources) {
    assertThat(resources, hasSize(1));
    DeployedResource resource = resources.iterator().next();
    var createdVNet = azureResourceManager.networks().getById(resource.resourceId());
    assertThat(createdVNet.id(), equalTo(resource.resourceId()));
    assertThat(
        createdVNet.tags().get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()),
        equalTo(landingZoneId));
    assertThat(
        createdVNet.tags().get(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString()),
        equalTo("compute"));
    assertThat(
        createdVNet.tags().get(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET.toString()),
        equalTo("storage"));
  }

  @Test
  void deploy_privateStorageInVNetUsingPrerequisitesDeployment() {

    var deployment = landingZoneDeployments.define(landingZoneId);

    var vNet =
        azureResourceManager
            .networks()
            .define(vNetName)
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup)
            .withAddressSpace("10.0.0.0/28")
            .withSubnet("subnet1", "10.0.0.0/29");

    var storage =
        azureResourceManager
            .storageAccounts()
            .define(storageAccountName)
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup);

    var prerequisites =
        deployment
            .definePrerequisites()
            .withVNetWithPurpose(vNet, "subnet1", SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET)
            .withResourceWithPurpose(storage, ResourcePurpose.SHARED_RESOURCE)
            .deploy();

    var vNetId = TestUtils.findFirstVNetId(prerequisites);
    var storageId = TestUtils.findFirstStorageAccountId(prerequisites);
    var deployedVNet = azureResourceManager.networks().getById(vNetId);

    var privateEndpoint =
        azureResourceManager
            .privateEndpoints()
            .define(TestArmResourcesFactory.createUniqueAzureResourceName())
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup)
            .withSubnetId(deployedVNet.subnets().get("subnet1").id())
            .definePrivateLinkServiceConnection(
                TestArmResourcesFactory.createUniqueAzureResourceName())
            .withResourceId(storageId)
            .withSubResource(PrivateLinkSubResourceName.STORAGE_BLOB)
            .attach();

    var privateEndpointResource = deployment.withResource(privateEndpoint).deploy();

    assertThatStorageWithPrivateEndpointInVNetIsCreated(
        prerequisites, vNetId, storageId, privateEndpointResource);
  }

  private void assertThatStorageWithPrivateEndpointInVNetIsCreated(
      List<DeployedResource> prerequisites,
      String vNetId,
      String storageId,
      List<DeployedResource> privateEndpointResource) {
    var privateEndpointId = privateEndpointResource.iterator().next().resourceId();
    assertThat(prerequisites, hasSize(2));
    assertThat(privateEndpointResource, hasSize(1));
    var deployedNetwork = azureResourceManager.networks().getById(vNetId);
    var deployedStorage = azureResourceManager.storageAccounts().getById(storageId);
    var deployedPrivateEndpoint =
        azureResourceManager.privateEndpoints().getById(privateEndpointId);

    assertThat(deployedNetwork.id(), equalTo(vNetId));
    assertThat(deployedStorage.id(), equalTo(storageId));
    assertThat(deployedPrivateEndpoint.id(), equalTo(privateEndpointId));
  }

  @Test
  void deploy_duplicateDeploymentsSucceedWithRetries_OnlyOneResourceIsCreated() {
    var storage1 =
        azureResourceManager
            .storageAccounts()
            .define(storageAccountName)
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup);

    var storage2 =
        azureResourceManager
            .storageAccounts()
            .define(storageAccountName)
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup);

    var first =
        landingZoneDeployments
            .define(landingZoneId)
            .withResourceWithPurpose(storage1, ResourcePurpose.SHARED_RESOURCE)
            .deployAsync()
            .retryWhen(Retry.max(1));

    var second =
        landingZoneDeployments
            .define(landingZoneId)
            .withResourceWithPurpose(storage2, ResourcePurpose.SHARED_RESOURCE)
            .deployAsync()
            .retryWhen(Retry.max(1));

    // using merge makes the subscription eagerly hence these would be concurrent calls.
    List<DeployedResource> result =
        Flux.merge(first, second).toStream().collect(Collectors.toList());

    assertThat(result, hasSize(2));
    assertThat(result.get(0), equalTo(result.get(1)));
    var existingStorage =
        azureResourceManager.storageAccounts().getById(result.get(0).resourceId());
    assertThat(existingStorage.id(), equalTo(result.get(1).resourceId()));
  }
}
