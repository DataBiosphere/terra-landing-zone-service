package bio.terra.landingzone.stairway.flight.create;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.landingzone.common.k8s.configmap.reader.ContainerLogV2ConfigMapValidator;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.LandingZoneJobBuilder;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.exception.InternalStairwayException;
import bio.terra.landingzone.job.exception.JobNotFoundException;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.TestArmResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.DeleteRulesVerifier;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.ResourcesDeleteManager;
import bio.terra.landingzone.library.landingzones.management.deleterules.*;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDiagnosticSetting;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.resource.step.AggregateLandingZoneResourcesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksCostOptimizationDataCollectionRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsDataCollectionRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsWorkspaceStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelAlertRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAuditLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.landingzone.stairway.flight.create.resource.step.KubernetesClientProviderImpl;
import bio.terra.landingzone.stairway.flight.utils.AlertRulesHelper;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.FlightNotFoundException;
import bio.terra.stairway.exception.StairwayException;
import com.azure.resourcemanager.batch.models.*;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.monitor.fluent.models.DataCollectionRuleResourceInner;
import com.azure.resourcemanager.monitor.models.LogSettings;
import com.azure.resourcemanager.storage.models.PublicAccess;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Tag("integration")
@ActiveProfiles("test")
@PropertySource(value = "classpath:integration_azure_env.properties", ignoreResourceNotFound = true)
@ExtendWith(SpringExtension.class)
@SpringBootTest()
@SpringBootApplication(
    scanBasePackages = {
      "bio.terra.common.logging",
      "bio.terra.common.migrate",
      "bio.terra.common.kubernetes",
      "bio.terra.common.stairway",
      "bio.terra.landingzone"
    },
    exclude = {DataSourceAutoConfiguration.class})
@EnableRetry
@EnableTransactionManagement
public class CreateLandingZoneResourcesFlightIntegrationTest extends BaseIntegrationTest {
  private static final int LANDING_ZONE_RESOURCES_DELETED_AWAIT_TIMEOUT_MINUTES = 2;
  private static final int LANDING_ZONE_RESOURCES_AVAILABLE_AWAIT_TIMEOUT_MINUTES = 2;
  // includes time for rollback too in case of any issues
  private static final int LZ_CREATED_AWAIT_TIMEOUT_MINUTES = 30;
  private static final int LZ_DELETED_AWAIT_TIMEOUT_MINUTES = 20;

  @Mock private BearerToken bearerToken;
  @MockBean private LandingZoneDao landingZoneDao;

  @Autowired LandingZoneService landingZoneService;
  @Autowired LandingZoneJobService azureLandingZoneJobService;

  @Autowired
  @Qualifier("landingZoneStairwayComponent")
  StairwayComponent stairwayComponent;

  UUID jobId;
  UUID landingZoneId;
  ProfileModel profile;
  LandingZoneManager landingZoneManager;

  @BeforeAll
  static void init() {
    Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
  }

  @BeforeEach
  void setup() {

    // we need to use isolated resource group for each test
    resourceGroup =
        TestArmResourcesFactory.createTestResourceGroup(armManagers.azureResourceManager());

    jobId = UUID.randomUUID();
    landingZoneId = UUID.randomUUID();

    profile =
        new ProfileModel()
            .managedResourceGroupId(resourceGroup.name())
            .subscriptionId(UUID.fromString(azureProfile.getSubscriptionId()))
            .tenantId(UUID.fromString(azureProfile.getTenantId()))
            .cloudPlatform(CloudPlatform.AZURE)
            .description("dummyProfile")
            .id(UUID.randomUUID());
    landingZoneManager =
        LandingZoneManager.createLandingZoneManager(
            tokenCredential,
            azureProfile,
            resourceGroup.name(),
            null /*ignore this value in test*/);
  }

  @AfterEach
  void cleanUpResources() {
    armManagers.azureResourceManager().resourceGroups().deleteByName(resourceGroup.name());
  }

  @Test
  void createResourcesFlightDeploysCromwellResources() {
    String resultPath = "";

    var request =
        LandingZoneRequestFixtures.createCromwellLZRequest(landingZoneId, profile.getId());

    landingZoneService.startLandingZoneResourceCreationJob(
        jobId.toString(), request, profile, landingZoneId, bearerToken, resultPath);

    await()
        .atMost(Duration.ofMinutes(LZ_CREATED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(
            () -> {
              var flightState = retrieveFlightState(jobId.toString());
              assertThat(flightState.getFlightStatus(), not(FlightStatus.RUNNING));
              var resources =
                  landingZoneManager.reader().listSharedResources(landingZoneId.toString());
              assertThat(
                  resources,
                  hasSize(
                      AggregateLandingZoneResourcesStep.deployedResourcesKeys.size()
                          + 2 /*magic number which represents data collection rules which probably should not be tagged as shared resources*/));
            });
    var flightState = retrieveFlightState(jobId.toString());
    assertThat(flightState.getFlightStatus(), is(FlightStatus.SUCCESS));

    // verify that we have our diagnostic settings setup as expected
    // (these can silently fail to create, so we are making a explicit check here)
    assertDiagnosticSettings(
        CreateStorageAuditLogSettingsStep.STORAGE_AUDIT_LOG_SETTINGS_KEY, flightState);
    assertAksCostOptimizedDataCollectionRule(flightState);
    assertContainerLogV2();
    testCannotDeleteLandingZoneWithDependencies();
  }

  void assertContainerLogV2() {
    // names below correspond to what we have in ContainerLogV2.yaml
    String containerLogV2ConfigMapName = "container-azm-ms-agentconfig";
    String containerLogV2ConfigMapNamespace = "kube-system";
    var resources = landingZoneManager.reader().listSharedResources(landingZoneId.toString());
    String aksResourceId =
        resources.stream()
            .filter(r -> r.resourceType().contains("Microsoft.ContainerService/managedClusters"))
            .findFirst()
            .orElseThrow()
            .resourceId();
    String[] aksResourceNameParts = aksResourceId.split("/");
    CoreV1Api k8sApiClient =
        new KubernetesClientProviderImpl()
            .createCoreApiClient(
                armManagers,
                resourceGroup.name(),
                aksResourceNameParts[aksResourceNameParts.length - 1] /* aks cluster name*/);
    try {
      // validate that corresponding ConfigMap exists and contains required value
      V1ConfigMap configMap =
          k8sApiClient.readNamespacedConfigMap(
              containerLogV2ConfigMapName, containerLogV2ConfigMapNamespace, null);
      ContainerLogV2ConfigMapValidator.validate(configMap);
    } catch (ApiException ex) {
      fail(
          String.format(
              "Failed to find ConfigMap with name '%s'. ConfigMap is required to enable ContainerLogV2.",
              containerLogV2ConfigMapName),
          ex);
    }
  }

  void assertDiagnosticSettings(String settingsResultKey, FlightState flightState) {
    var results = flightState.getResultMap().orElseThrow();
    var expectedDiagnosticSettings =
        results.get(settingsResultKey, LandingZoneDiagnosticSetting.class);
    assertNotNull(expectedDiagnosticSettings);

    var diagnosticSettings =
        armManagers
            .monitorManager()
            .diagnosticSettings()
            .listByResource(expectedDiagnosticSettings.resourceId())
            .stream()
            .toList();

    assertThat(diagnosticSettings.size(), equalTo(1));
    var actualLogCategories =
        diagnosticSettings.get(0).logs().stream().map(LogSettings::category).toList();
    var expectedLogCategories =
        expectedDiagnosticSettings.logs().stream().map(LogSettings::category).toList();
    assertEquals(actualLogCategories, expectedLogCategories);
  }

  void assertAksCostOptimizedDataCollectionRule(FlightState flightState) {
    var results = flightState.getResultMap().orElseThrow();
    var dataCollectionRuleId =
        results.get(
            CreateAksCostOptimizationDataCollectionRulesStep
                .AKS_COST_OPTIMIZATION_DATA_COLLECTION_RULE_ID,
            String.class);
    assertNotNull(dataCollectionRuleId);
    var mrg =
        results.get(GetManagedResourceGroupInfo.TARGET_MRG_KEY, TargetManagedResourceGroup.class);
    assertNotNull(mrg);

    var dataCollectionRuleIdParts = dataCollectionRuleId.split("/");
    var dataCollectionRuleName = dataCollectionRuleIdParts[dataCollectionRuleIdParts.length - 1];
    DataCollectionRuleResourceInner rule =
        armManagers
            .monitorManager()
            .serviceClient()
            .getDataCollectionRules()
            .getByResourceGroup(mrg.name(), dataCollectionRuleName);
    assertNotNull(rule);
    var ruleDataSources = rule.dataSources();
    assertNotNull(ruleDataSources);
    var ruleDataSourcesExtensions = ruleDataSources.extensions();
    assertNotNull(ruleDataSourcesExtensions);
    assertThat(ruleDataSourcesExtensions.size(), equalTo(1));
    var extensionDataSource = ruleDataSourcesExtensions.get(0);
    assertThat(extensionDataSource.extensionName(), equalTo("ContainerInsights"));
    assertThat(extensionDataSource.name(), equalTo("ContainerInsightsExtension"));
    var containerInsightsExtensionSettings = extensionDataSource.extensionSettings();
    assertNotNull(containerInsightsExtensionSettings);
    var dataCollectionSettings =
        ((LinkedHashMap) containerInsightsExtensionSettings).get("dataCollectionSettings");
    assertNotNull(dataCollectionSettings);
    var interval = ((LinkedHashMap) dataCollectionSettings).get("interval");
    assertNotNull(interval);
    assertThat(
        interval,
        equalTo(CreateAksCostOptimizationDataCollectionRulesStep.DATA_COLLECTION_INTERVAL));
    var namespaceFilteringMode =
        ((LinkedHashMap) dataCollectionSettings).get("namespaceFilteringMode");
    assertNotNull(namespaceFilteringMode);
    assertThat(
        namespaceFilteringMode,
        equalTo(
            CreateAksCostOptimizationDataCollectionRulesStep
                .DATA_COLLECTION_NAMESPACE_FILTERING_MODE));
  }

  private void testCannotDeleteLandingZoneWithDependencies() {
    createBlobContainer();
    createHybridConnection();
    createDatabase();
    scaleNodePool();
    createVirtualMachine();
    createBatchPool();

    var deleteManager =
        new ResourcesDeleteManager(armManagers, new DeleteRulesVerifier(armManagers));
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

  private void createBlobContainer() {
    var storageAccount =
        armManagers
            .azureResourceManager()
            .storageAccounts()
            .listByResourceGroup(resourceGroup.name())
            .stream()
            .filter(sa -> inLandingZone(landingZoneId, sa.tags()))
            .findFirst()
            .orElseThrow();

    armManagers
        .azureResourceManager()
        .storageBlobContainers()
        .defineContainer("temp")
        .withExistingStorageAccount(storageAccount)
        .withPublicAccess(PublicAccess.NONE)
        .create();
  }

  private void createHybridConnection() {
    var azureRelay =
        armManagers.relayManager().namespaces().listByResourceGroup(resourceGroup.name()).stream()
            .filter(n -> inLandingZone(landingZoneId, n.tags()))
            .findFirst()
            .orElseThrow();

    armManagers
        .relayManager()
        .hybridConnections()
        .define("myhc")
        .withExistingNamespace(resourceGroup.name(), azureRelay.name())
        .create();
  }

  private void createDatabase() {
    var postgresServer =
        armManagers.postgreSqlManager().servers().listByResourceGroup(resourceGroup.name()).stream()
            .filter(pg -> inLandingZone(landingZoneId, pg.tags()))
            .findFirst()
            .orElseThrow();

    armManagers
        .postgreSqlManager()
        .databases()
        .define("mydb")
        .withExistingFlexibleServer(resourceGroup.name(), postgresServer.name())
        .create();
  }

  private void scaleNodePool() {
    var aksCluster =
        armManagers
            .azureResourceManager()
            .kubernetesClusters()
            .listByResourceGroup(resourceGroup.name())
            .stream()
            .filter(kc -> inLandingZone(landingZoneId, kc.tags()))
            .findFirst()
            .orElseThrow();

    var nodePoolName = aksCluster.agentPools().values().stream().findFirst().orElseThrow();

    aksCluster
        .update()
        .updateAgentPool(nodePoolName.name())
        .withAgentPoolVirtualMachineCount(2)
        .parent()
        .apply();
  }

  private void createVirtualMachine() {
    var deployedSubnet =
        landingZoneManager
            .reader()
            .listSubnetsBySubnetPurpose(
                landingZoneId.toString(), SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET)
            .stream()
            .findFirst()
            .orElseThrow();
    var vNet = armManagers.azureResourceManager().networks().getById(deployedSubnet.vNetId());

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
  }

  private void createBatchPool() {
    var batch =
        armManagers
            .batchManager()
            .batchAccounts()
            .listByResourceGroup(resourceGroup.name())
            .stream()
            .filter(ba -> inLandingZone(landingZoneId, ba.tags()))
            .findFirst()
            .orElseThrow();

    var pool =
        armManagers
            .batchManager()
            .pools()
            .define("poolName")
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

  @NotNull
  private static Boolean inLandingZone(UUID landingZoneId, Map<String, String> tags) {
    return tags.getOrDefault(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), "")
        .equalsIgnoreCase(landingZoneId.toString());
  }

  @Test
  void doesSomething() throws InterruptedException {
    System.out.println("********** STARTING TEST");
    var lzId = UUID.randomUUID();
    var parametersResolver = new ParametersResolver(Map.of(), LandingZoneDefaultParameters.get());
    var resourceNameProvider = new ResourceNameProvider(lzId);
    var lawStep =
        new CreateLogAnalyticsWorkspaceStep(armManagers, parametersResolver, resourceNameProvider);
    var context = mock(FlightContext.class, RETURNS_SMART_NULLS);
    var inputParams = new FlightMap();
    inputParams.put(LandingZoneFlightMapKeys.BILLING_PROFILE, new ProfileModel());
    inputParams.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId);
    var workingMap = new FlightMap();
    var targetMrg =
        new TargetManagedResourceGroup(resourceGroup.name(), resourceGroup.regionName());
    workingMap.put(GetManagedResourceGroupInfo.TARGET_MRG_KEY, targetMrg);
    when(context.getInputParameters()).thenReturn(inputParams);
    when(context.getWorkingMap()).thenReturn(workingMap);

    System.out.println("****** STARTING LAW STEP");
    var lawResult = lawStep.doStep(context);
    assertThat(lawResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED LAW STEP");

    System.out.println("****** STARTING STORAGE ACCT STEP");
    var saStep =
        new CreateStorageAccountStep(armManagers, parametersResolver, resourceNameProvider);
    var saResult = saStep.doStep(context);
    assertThat(saResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED STORAGE ACCT STEP");

    System.out.println("****** STARTING BATCH ACCT STEP");
    var baStep = new CreateBatchAccountStep(armManagers, parametersResolver, resourceNameProvider);
    var baResult = baStep.doStep(context);
    assertThat(baResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED BATCH ACCT STEP");

    System.out.println("****** STARTING DCRULES STEP");
    var dcRulesStep =
        new CreateLogAnalyticsDataCollectionRulesStep(
            armManagers, parametersResolver, resourceNameProvider);
    var dcRulesResult = dcRulesStep.doStep(context);
    assertThat(dcRulesResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED DCRULES STEP");

    System.out.println("****** STARTING STORAGE ACCT AUDIT LOG SETTINGS STEP");
    var saDcRulesStep =
        new CreateStorageAuditLogSettingsStep(
            armManagers, parametersResolver, resourceNameProvider);
    var saDcRulesResult = saDcRulesStep.doStep(context);
    assertThat(saDcRulesResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED STORAGE ACCT AUDIT LOG SETTINGS STEP");

    System.out.println("****** STARTING BATCH LOG SETTINGS STEP");
    var baRulesStep =
        new CreateBatchLogSettingsStep(armManagers, parametersResolver, resourceNameProvider);
    var baRulesResult = baRulesStep.doStep(context);
    assertThat(baRulesResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED BATCH LOG SETTINGS STEP");

    System.out.println("****** STARTING CREATE SENTINEL STEP");
    var sentinelStep =
        new CreateSentinelStep(armManagers, parametersResolver, resourceNameProvider);
    var sentinelResult = sentinelStep.doStep(context);
    assertThat(sentinelResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED CREATE SENTINEL STEP");

    System.out.println("****** STARTING CREATE ALERT RULES STEP");
    var alertRulesHelper = new AlertRulesHelper(armManagers.securityInsightsManager());
    var protectedDataConfig =
        mock(LandingZoneProtectedDataConfiguration.class, RETURNS_SMART_NULLS);
    when(protectedDataConfig.getLogicAppResourceId())
        .thenReturn(
            "/subscriptions/c5f8eca3-f512-48cb-b01f-f19f1af9014c/resourceGroups/terra-dev-audit-logs-rg/providers/Microsoft.Logic/workflows/terra-protected-data-slack-notification");
    when(protectedDataConfig.getSentinelScheduledAlertRuleTemplateIds())
        .thenReturn(
            List.of(
                "0b9ae89d-8cad-461c-808f-0494f70ad5c4",
                "8ee967a2-a645-4832-85f4-72b635bcb3a6",
                "1ce5e766-26ab-4616-b7c8-3b33ae321e80",
                "532f62c1-fba6-4baa-bbb6-4a32a4ef32fa",
                "e1ce0eab-10d1-4aae-863f-9a383345ba88",
                "90d3f6ec-80fb-48e0-9937-2c70c9df9bad",
                "80733eb7-35b2-45b6-b2b8-3c51df258206",
                "b31037ea-6f68-4fbd-bab2-d0d0f44c2fcf",
                "e7ec9fa6-e7f7-41ed-a34b-b956837a3ee6"));
    when(protectedDataConfig.getSentinelMlRuleTemplateIds())
        .thenReturn(List.of("fa118b98-de46-4e94-87f9-8e6d5060b60b"));
    when(protectedDataConfig.getSentinelNrtRuleTemplateIds())
        .thenReturn(List.of("dd03057e-4347-4853-bf1e-2b2d21eb4e59"));

    var alertRulesStep =
        new CreateSentinelAlertRulesStep(
            armManagers,
            parametersResolver,
            resourceNameProvider,
            alertRulesHelper,
            protectedDataConfig);
    var alertRulesResult = alertRulesStep.doStep(context);
    assertThat(alertRulesResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    System.out.println("****** FINISHED CREATE ALERT RULES STEP");
  }

  @Test
  void createResourcesFlightDeploysProtectedDataResourcesAndDeleteIt() {
    // Step 1 - create lz
    var request =
        LandingZoneRequestFixtures.createProtectedDataLZRequest(landingZoneId, profile.getId());

    landingZoneService.startLandingZoneResourceCreationJob(
        jobId.toString(), request, profile, landingZoneId, bearerToken, "");

    await()
        .atMost(Duration.ofMinutes(LZ_CREATED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertFlightChangeStatusFromRunning(jobId.toString()));
    var flightState = retrieveFlightState(jobId.toString());
    assertThat(flightState.getFlightStatus(), is(FlightStatus.SUCCESS));

    await()
        .atMost(Duration.ofMinutes(LANDING_ZONE_RESOURCES_AVAILABLE_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertLandingZoneSharedResourcesExisted(landingZoneId));

    // Step 2 - delete lz
    var existingLandingZoneRecord =
        LandingZoneRequestFixtures.createLandingZoneRecord(
            landingZoneId, resourceGroup.name(), azureProfile, profile);
    // even with isolated Flight for resource deletion we need to mock dao interaction
    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(existingLandingZoneRecord);

    UUID deleteJobId = UUID.randomUUID();
    startLandingZoneResourceDeletionFlight(deleteJobId.toString(), landingZoneId, bearerToken, "");
    await()
        .atMost(Duration.ofMinutes(LZ_DELETED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertFlightChangeStatusFromRunning(deleteJobId.toString()));
    var deleteFlightState = retrieveFlightState(deleteJobId.toString());
    assertThat(deleteFlightState.getFlightStatus(), is(FlightStatus.SUCCESS));

    await()
        .atMost(Duration.ofMinutes(LANDING_ZONE_RESOURCES_DELETED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertLandingZoneResourcesDeleted(landingZoneId));
  }

  // just a clone of the method in LandingZoneService
  // but using the stairwayComponent directly means we don't have to fuss around with
  // authentication,
  // which brings in the requirement for a lot more mocking
  private FlightState retrieveFlightState(String jobId) {
    try {
      return stairwayComponent.get().getFlightState(jobId);
    } catch (FlightNotFoundException flightNotFoundException) {
      throw new JobNotFoundException(
          "The flight " + jobId + " was not found", flightNotFoundException);
    } catch (StairwayException | InterruptedException stairwayEx) {
      throw new InternalStairwayException(stairwayEx);
    }
  }

  private void startLandingZoneResourceDeletionFlight(
      String jobId, UUID landingZoneId, BearerToken bearerToken, String resultPath) {
    String jobDescription = "Deleting Azure Landing Zone. Landing Zone ID:%s";
    final LandingZoneJobBuilder jobBuilder =
        azureLandingZoneJobService
            .newJob()
            .jobId(jobId)
            .description(String.format(jobDescription, landingZoneId.toString()))
            // Real deletion flight contains SAM and DB interaction and LZ resources clean up.
            // This flight is only limited to cleaning up the specific LZ resources.
            .flightClass(TestDeleteLandingZoneResourcesFlight.class)
            .operationType(OperationType.DELETE)
            .bearerToken(bearerToken)
            .addParameter(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    jobBuilder.submit();
  }

  private void assertFlightChangeStatusFromRunning(String jobId) {
    var flightState = retrieveFlightState(jobId);
    assertThat(flightState.getFlightStatus(), not(FlightStatus.RUNNING));
  }

  private void assertLandingZoneResourcesDeleted(UUID landingZoneId) {
    var resources = landingZoneManager.reader().listAllResources(landingZoneId.toString());
    assertThat(resources, hasSize(0));
  }

  private void assertLandingZoneSharedResourcesExisted(UUID landingZoneId) {
    var resources = landingZoneManager.reader().listAllResources(landingZoneId.toString());
    assertFalse(resources.isEmpty());
  }
}
