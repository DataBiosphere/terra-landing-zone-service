package bio.terra.landingzone.library.landingzones.management;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.landingzone.library.landingzones.LandingZoneTestFixture;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.deleterules.AKSAgentPoolHasMoreThanOneNode;
import bio.terra.landingzone.library.landingzones.management.deleterules.AzureRelayHasHybridConnections;
import bio.terra.landingzone.library.landingzones.management.deleterules.BatchAccountHasNodePools;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import bio.terra.landingzone.library.landingzones.management.deleterules.PostgreSQLServerHasDBs;
import bio.terra.landingzone.library.landingzones.management.deleterules.StorageAccountHasContainers;
import bio.terra.landingzone.library.landingzones.management.deleterules.VmsAreAttachedToVnet;
import com.azure.resourcemanager.batch.models.AutoScaleSettings;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.DeploymentConfiguration;
import com.azure.resourcemanager.batch.models.ImageReference;
import com.azure.resourcemanager.batch.models.ScaleSettings;
import com.azure.resourcemanager.batch.models.VirtualMachineConfiguration;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.postgresql.models.Database;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.relay.models.HybridConnection;
import com.azure.resourcemanager.relay.models.RelayNamespace;
import com.azure.resourcemanager.storage.models.BlobContainer;
import com.azure.resourcemanager.storage.models.PublicAccess;
import com.azure.resourcemanager.storage.models.StorageAccount;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class ResourcesDeleteManagerTest extends LandingZoneTestFixture {

  private static KubernetesCluster aksCluster;
  private static StorageAccount storageAccount;
  private static RelayNamespace azureRelay;
  private static Network vNet;
  private static BatchAccount batch;

  private ResourcesDeleteManager deleteManager;

  private static Server postgresServer;

  private static UUID landingZoneId;

  private static DeployedSubnet deployedSubnet;

  @BeforeAll
  static void setUpLandingZone() {
    LandingZoneManager landingZoneManager =
        LandingZoneManager.createLandingZoneManager(
            tokenCredential, azureProfile, resourceGroup.name());

    landingZoneId = UUID.randomUUID();

    List<DeployedResource> resources =
        landingZoneManager.deployLandingZone(
            landingZoneId.toString(),
            CromwellBaseResourcesFactory.class.getSimpleName(),
            DefinitionVersion.V1,
            null);

    postgresServer =
        LandingZoneTestFixture.armManagers
            .postgreSqlManager()
            .servers()
            .getById(
                landingZoneManager.reader().listSharedResources(landingZoneId.toString()).stream()
                    .filter(
                        r ->
                            r.resourceType()
                                .equalsIgnoreCase(
                                    AzureResourceTypeUtils.AZURE_POSTGRESQL_SERVER_TYPE))
                    .findFirst()
                    .orElseThrow()
                    .resourceId());

    aksCluster =
        LandingZoneTestFixture.armManagers
            .azureResourceManager()
            .kubernetesClusters()
            .getById(
                resources.stream()
                    .filter(
                        r ->
                            r.resourceType()
                                .equalsIgnoreCase(AzureResourceTypeUtils.AZURE_AKS_TYPE))
                    .findFirst()
                    .orElseThrow()
                    .resourceId());

    storageAccount =
        LandingZoneTestFixture.armManagers
            .azureResourceManager()
            .storageAccounts()
            .listByResourceGroup(resourceGroup.name())
            .stream()
            .filter(
                s ->
                    s.tags()
                        .getOrDefault(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), "")
                        .equalsIgnoreCase(landingZoneId.toString()))
            .findFirst()
            .orElseThrow();

    azureRelay =
        LandingZoneTestFixture.armManagers
            .relayManager()
            .namespaces()
            .getById(
                resources.stream()
                    .filter(
                        r ->
                            r.resourceType()
                                .equalsIgnoreCase(AzureResourceTypeUtils.AZURE_RELAY_TYPE))
                    .findFirst()
                    .orElseThrow()
                    .resourceId());

    deployedSubnet =
        landingZoneManager
            .reader()
            .listSubnetsBySubnetPurpose(
                landingZoneId.toString(), SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET)
            .stream()
            .findFirst()
            .orElseThrow();
    vNet =
        LandingZoneTestFixture.armManagers
            .azureResourceManager()
            .networks()
            .getById(deployedSubnet.vNetId());

    batch =
        LandingZoneTestFixture.armManagers
            .batchManager()
            .batchAccounts()
            .getById(
                resources.stream()
                    .filter(
                        r ->
                            r.resourceType()
                                .equalsIgnoreCase(AzureResourceTypeUtils.AZURE_BATCH_TYPE))
                    .findFirst()
                    .orElseThrow()
                    .resourceId());
  }

  @BeforeEach
  void setUp() {
    deleteManager = new ResourcesDeleteManager(armManagers, new DeleteRulesVerifier(armManagers));
  }

  @Test
  void landingZoneWithDependencies_cannotDelete() {
    BlobContainer container = createBlobContainer();
    HybridConnection hc = createHybridConnection();
    Database db = createDatabase();
    scaleNodePool();
    VirtualMachine vm = createVirtualMachine();
    createBatchPool("mypool");

    Exception exception =
        assertThrows(
            LandingZoneRuleDeleteException.class,
            () ->
                deleteManager.deleteLandingZoneResources(
                    landingZoneId.toString(), resourceGroup.name()));

    assertThat(
        exception.getMessage(),
        containsStringIgnoringCase(StorageAccountHasContainers.class.getSimpleName()));
    assertThat(
        exception.getMessage(),
        containsStringIgnoringCase(AzureRelayHasHybridConnections.class.getSimpleName()));
    assertThat(
        exception.getMessage(),
        containsStringIgnoringCase(PostgreSQLServerHasDBs.class.getSimpleName()));
    assertThat(
        exception.getMessage(),
        containsStringIgnoringCase(AKSAgentPoolHasMoreThanOneNode.class.getSimpleName()));
    assertThat(
        exception.getMessage(),
        containsStringIgnoringCase(VmsAreAttachedToVnet.class.getSimpleName()));
    assertThat(
        exception.getMessage(),
        containsStringIgnoringCase(BatchAccountHasNodePools.class.getSimpleName()));
  }

  private BlobContainer createBlobContainer() {
    var container =
        armManagers
            .azureResourceManager()
            .storageBlobContainers()
            .defineContainer("temp")
            .withExistingStorageAccount(storageAccount)
            .withPublicAccess(PublicAccess.NONE)
            .create();
    return container;
  }

  private HybridConnection createHybridConnection() {
    var hc =
        armManagers
            .relayManager()
            .hybridConnections()
            .define("myhc")
            .withExistingNamespace(resourceGroup.name(), azureRelay.name())
            .create();
    return hc;
  }

  private Database createDatabase() {
    var db =
        armManagers
            .postgreSqlManager()
            .databases()
            .define("mydb")
            .withExistingServer(resourceGroup.name(), postgresServer.name())
            .create();
    return db;
  }

  private void scaleNodePool() {
    var nodePoolName = aksCluster.agentPools().values().stream().findFirst().orElseThrow();

    aksCluster
        .update()
        .updateAgentPool(nodePoolName.name())
        .withAgentPoolVirtualMachineCount(2)
        .parent()
        .apply();
  }

  private VirtualMachine createVirtualMachine() {
    var vm =
        armManagers
            .azureResourceManager()
            .virtualMachines()
            .define("myvm")
            .withRegion(resourceGroup.regionName())
            .withExistingResourceGroup(resourceGroup)
            .withExistingPrimaryNetwork(vNet)
            .withSubnet(deployedSubnet.name())
            .withPrimaryPrivateIPAddressDynamic()
            .withoutPrimaryPublicIPAddress()
            .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
            .withRootUsername("username")
            .withRootPassword(UUID.randomUUID().toString())
            .withSize(VirtualMachineSizeTypes.STANDARD_D2_V3)
            .create();
    return vm;
  }

  private void createBatchPool(String poolName) {
    var pool =
        armManagers
            .batchManager()
            .pools()
            .define(poolName)
            .withExistingBatchAccount(resourceGroup.name(), batch.name())
            .withVmSize("Standard_D2as_v4")
            .withDeploymentConfiguration(
                new DeploymentConfiguration()
                    .withVirtualMachineConfiguration(
                        new VirtualMachineConfiguration()
                            .withImageReference(
                                new ImageReference()
                                    .withPublisher("Canonical")
                                    .withOffer("UbuntuServer")
                                    .withSku("18.04-LTS")
                                    .withVersion("latest"))
                            .withNodeAgentSkuId("batch.node.ubuntu 18.04")))
            .withScaleSettings(
                new ScaleSettings()
                    .withAutoScale(
                        new AutoScaleSettings()
                            .withFormula("$TargetDedicatedNodes=1")
                            .withEvaluationInterval(Duration.parse("PT5M"))))
            .create();
  }
}
