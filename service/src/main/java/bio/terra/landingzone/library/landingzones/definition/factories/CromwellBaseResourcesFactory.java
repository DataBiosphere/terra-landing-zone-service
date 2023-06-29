package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionContext;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.LandingZoneDefinable;
import bio.terra.landingzone.library.landingzones.definition.LandingZoneDefinition;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.ParametersExtractor;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.AksParametersValidator;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.BlobCorsParametersValidator;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment.DefinitionStages.Deployable;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment.DefinitionStages.WithLandingZoneResource;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.AzureResourceTypeUtils;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.applicationinsights.models.ApplicationType;
import com.azure.resourcemanager.containerservice.models.AgentPoolMode;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import com.azure.resourcemanager.containerservice.models.ManagedClusterAddonProfile;
import com.azure.resourcemanager.monitor.fluent.models.DataCollectionRuleResourceInner;
import com.azure.resourcemanager.monitor.models.DataCollectionRuleDataSources;
import com.azure.resourcemanager.monitor.models.DataCollectionRuleDestinations;
import com.azure.resourcemanager.monitor.models.DataFlow;
import com.azure.resourcemanager.monitor.models.KnownDataFlowStreams;
import com.azure.resourcemanager.monitor.models.KnownPerfCounterDataSourceStreams;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceFacilityNames;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceLogLevels;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceStreams;
import com.azure.resourcemanager.monitor.models.LogAnalyticsDestination;
import com.azure.resourcemanager.monitor.models.PerfCounterDataSource;
import com.azure.resourcemanager.monitor.models.SyslogDataSource;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.PrivateLinkSubResourceName;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerVersion;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Sku;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.storage.models.CorsRule;
import com.azure.resourcemanager.storage.models.CorsRuleAllowedMethodsItem;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An implementation of {@link LandingZoneDefinitionFactory} that deploys resources required for
 * cromwell. Current resources are: - VNet: Subnets required for AKS, Batch, PostgreSQL and
 * Compute/VMs - AKS Account (?) TODO - AKS Nodepool TODO - Batch Account TODO - Storage Account
 * TODO - PostgreSQL server TODO
 */
public class CromwellBaseResourcesFactory extends ArmClientsDefinitionFactory {
  private final String LZ_NAME = "Cromwell Landing Zone Base Resources";
  private final String LZ_DESC =
      "Cromwell Base Resources: VNet, AKS Account & Nodepool, Batch Account,"
          + " Storage Account, PostgreSQL server, Subnets for AKS, Batch, Posgres, and Compute";

  public static final String STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT = "*";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT =
      "GET,HEAD,OPTIONS,PUT,PATCH,POST,MERGE,DELETE";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT =
      "authorization,content-type,x-app-id,Referer,x-ms-blob-type,x-ms-copy-source,content-length";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT = "";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT = "0";

  private AksParametersValidator aksValidator;
  private BlobCorsParametersValidator bcValidator;

  public enum Subnet {
    AKS_SUBNET,
    BATCH_SUBNET,
    POSTGRESQL_SUBNET,
    COMPUTE_SUBNET
  }

  public enum ParametersNames {
    POSTGRES_DB_ADMIN,
    POSTGRES_DB_PASSWORD,
    POSTGRES_SERVER_SKU,
    POSTGRES_SERVER_SKU_TIER,
    POSTGRES_SERVER_VERSION,
    POSTGRES_SERVER_BACKUP_RETENTION_DAYS,
    POSTGRES_SERVER_STORAGE_SIZE_GB,
    VNET_ADDRESS_SPACE,
    AUDIT_LOG_RETENTION_DAYS,
    AKS_NODE_COUNT,
    AKS_MACHINE_TYPE,
    AKS_AUTOSCALING_ENABLED,
    AKS_AUTOSCALING_MIN,
    AKS_AUTOSCALING_MAX,
    AKS_AAD_PROFILE_USER_GROUP_ID,
    STORAGE_ACCOUNT_SKU_TYPE
  }

  CromwellBaseResourcesFactory() {}

  public CromwellBaseResourcesFactory(ArmManagers armManagers) {
    super(armManagers);
    aksValidator = new AksParametersValidator();
    bcValidator = new BlobCorsParametersValidator();
  }

  @Override
  public DefinitionHeader header() {
    return new DefinitionHeader(LZ_NAME, LZ_DESC);
  }

  @Override
  public List<DefinitionVersion> availableVersions() {
    return List.of(DefinitionVersion.V1);
  }

  @Override
  public LandingZoneDefinable create(DefinitionVersion version) {
    if (version.equals(DefinitionVersion.V1)) {
      return new DefinitionV1(armManagers);
    }
    throw new RuntimeException("Invalid Version");
  }

  class DefinitionV1 extends LandingZoneDefinition {

    private final ClientLogger logger = new ClientLogger(DefinitionV1.class);

    protected DefinitionV1(ArmManagers armManagers) {
      super(armManagers);
    }

    @Override
    public Deployable definition(DefinitionContext definitionContext) {
      AzureResourceManager azureResourceManager = armManagers.azureResourceManager();
      WithLandingZoneResource deployment = definitionContext.deployment();
      ResourceGroup resourceGroup = definitionContext.resourceGroup();
      ResourceNameGenerator nameGenerator = definitionContext.resourceNameGenerator();
      ParametersResolver parametersResolver =
          new ParametersResolver(definitionContext.parameters(), getDefaultParameters());

      aksValidator.validate(parametersResolver);
      bcValidator.validate(parametersResolver);

      var logAnalyticsWorkspace =
          armManagers
              .logAnalyticsManager()
              .workspaces()
              .define(
                  nameGenerator.nextName(
                      ResourceNameGenerator.MAX_LOG_ANALYTICS_WORKSPACE_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name())
              .withRetentionInDays(
                  Integer.parseInt(
                      parametersResolver.getValue(
                          ParametersNames.AUDIT_LOG_RETENTION_DAYS.name())));

      var vNet =
          azureResourceManager
              .networks()
              .define(nameGenerator.nextName(ResourceNameGenerator.MAX_VNET_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup)
              .withAddressSpace(
                  parametersResolver.getValue(ParametersNames.VNET_ADDRESS_SPACE.name()))
              .withSubnet(
                  Subnet.AKS_SUBNET.name(), parametersResolver.getValue(Subnet.AKS_SUBNET.name()))
              .withSubnet(
                  Subnet.BATCH_SUBNET.name(),
                  parametersResolver.getValue(Subnet.BATCH_SUBNET.name()))
              .withSubnet(
                  Subnet.POSTGRESQL_SUBNET.name(),
                  parametersResolver.getValue(Subnet.POSTGRESQL_SUBNET.name()))
              .withSubnet(
                  Subnet.COMPUTE_SUBNET.name(),
                  parametersResolver.getValue(Subnet.COMPUTE_SUBNET.name()));

      var postgres =
          armManagers
              .postgreSqlManager()
              .servers()
              .define(
                  nameGenerator.nextName(ResourceNameGenerator.MAX_POSTGRESQL_SERVER_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name())
              .withVersion(ServerVersion.ONE_FOUR)
              .withAdministratorLogin(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_ADMIN.name()))
              .withAdministratorLoginPassword(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_PASSWORD.name()))
              .withSku(new Sku().withName("GP_Gen5_2"));

      String storageAccountName =
          nameGenerator.nextName(ResourceNameGenerator.MAX_STORAGE_ACCOUNT_NAME_LENGTH);
      var storage =
          azureResourceManager
              .storageAccounts()
              .define(storageAccountName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup);

      var batch =
          armManagers
              .batchManager()
              .batchAccounts()
              .define(nameGenerator.nextName(ResourceNameGenerator.MAX_BATCH_ACCOUNT_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name());

      var prerequisites =
          deployment
              .definePrerequisites()
              .withVNetWithPurpose(
                  vNet, Subnet.AKS_SUBNET.name(), SubnetResourcePurpose.AKS_NODE_POOL_SUBNET)
              .withVNetWithPurpose(
                  vNet, Subnet.BATCH_SUBNET.name(), SubnetResourcePurpose.WORKSPACE_BATCH_SUBNET)
              .withVNetWithPurpose(
                  vNet, Subnet.POSTGRESQL_SUBNET.name(), SubnetResourcePurpose.POSTGRESQL_SUBNET)
              .withVNetWithPurpose(
                  vNet,
                  Subnet.COMPUTE_SUBNET.name(),
                  SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET)
              .withResourceWithPurpose(postgres, ResourcePurpose.SHARED_RESOURCE)
              .withResourceWithPurpose(logAnalyticsWorkspace, ResourcePurpose.SHARED_RESOURCE)
              .withResourceWithPurpose(storage, ResourcePurpose.SHARED_RESOURCE)
              .withResourceWithPurpose(batch, ResourcePurpose.SHARED_RESOURCE)
              .deploy();

      var corsRules = buildStorageAccountBlobServiceCorsRules(parametersResolver);
      setupCorsForStorageAccountBlobService(
          azureResourceManager, resourceGroup.name(), storageAccountName, corsRules);

      String logAnalyticsWorkspaceId =
          getResourceId(prerequisites, AzureResourceTypeUtils.AZURE_LOG_ANALYTICS_WORKSPACE_TYPE);
      String vNetId = getResourceId(prerequisites, AzureResourceTypeUtils.AZURE_VNET_TYPE);
      String postgreSqlId = getResourceId(prerequisites, "Microsoft.DBforPostgreSQL/servers");
      String storageAccountId =
          getResourceId(prerequisites, AzureResourceTypeUtils.AZURE_STORAGE_ACCOUNT_TYPE);
      String batchAccountId = getResourceId(prerequisites, AzureResourceTypeUtils.AZURE_BATCH_TYPE);
      Network vNetwork = azureResourceManager.networks().getById(vNetId);

      createDataCollectionRule(definitionContext, logAnalyticsWorkspaceId);

      var privateEndpoint =
          azureResourceManager
              .privateEndpoints()
              .define(
                  nameGenerator.nextName(ResourceNameGenerator.MAX_PRIVATE_ENDPOINT_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup)
              .withSubnetId(vNetwork.subnets().get(Subnet.POSTGRESQL_SUBNET.name()).id())
              .definePrivateLinkServiceConnection(
                  nameGenerator.nextName(
                      ResourceNameGenerator.MAX_PRIVATE_LINK_CONNECTION_NAME_LENGTH))
              .withResourceId(postgreSqlId)
              .withSubResource(PrivateLinkSubResourceName.fromString("postgresqlServer"))
              .attach();

      final Map<String, ManagedClusterAddonProfile> addonProfileMap = new HashMap<>();
      addonProfileMap.put(
          "omsagent",
          new ManagedClusterAddonProfile()
              .withEnabled(true)
              .withConfig(Map.of("logAnalyticsWorkspaceResourceID", logAnalyticsWorkspaceId)));

      var aksPartial =
          azureResourceManager
              .kubernetesClusters()
              .define(nameGenerator.nextName(ResourceNameGenerator.MAX_AKS_CLUSTER_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup)
              .withDefaultVersion()
              .withSystemAssignedManagedServiceIdentity()
              .defineAgentPool(
                  nameGenerator.nextName(ResourceNameGenerator.MAX_AKS_AGENT_POOL_NAME_LENGTH))
              .withVirtualMachineSize(
                  ContainerServiceVMSizeTypes.fromString(
                      parametersResolver.getValue(ParametersNames.AKS_MACHINE_TYPE.name())))
              .withAgentPoolVirtualMachineCount(
                  Integer.parseInt(
                      parametersResolver.getValue(ParametersNames.AKS_NODE_COUNT.name())))
              .withAgentPoolMode(AgentPoolMode.SYSTEM)
              .withVirtualNetwork(vNetwork.id(), Subnet.AKS_SUBNET.name());

      if (Boolean.parseBoolean(
          parametersResolver.getValue(ParametersNames.AKS_AUTOSCALING_ENABLED.name()))) {
        int min =
            Integer.parseInt(
                parametersResolver.getValue(ParametersNames.AKS_AUTOSCALING_MIN.name()));
        int max =
            Integer.parseInt(
                parametersResolver.getValue(ParametersNames.AKS_AUTOSCALING_MAX.name()));
        aksPartial = aksPartial.withAutoScaling(min, max);
      }

      var aks =
          aksPartial
              .attach()
              .withDnsPrefix(
                  nameGenerator.nextName(ResourceNameGenerator.MAX_AKS_DNS_PREFIX_NAME_LENGTH))
              .withAddOnProfiles(addonProfileMap);

      var relay =
          armManagers
              .relayManager()
              .namespaces()
              .define(nameGenerator.nextName(ResourceNameGenerator.MAX_RELAY_NS_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name());

      var storageAuditLogSettings =
          armManagers
              .monitorManager()
              .diagnosticSettings()
              .define(
                  nameGenerator.nextName(ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH))
              .withResource(storageAccountId + "/blobServices/default")
              .withLogAnalytics(logAnalyticsWorkspaceId)
              .withLog("StorageRead", 0) // retention is handled by the log analytics workspace
              .withLog("StorageWrite", 0)
              .withLog("StorageDelete", 0);

      var batchLogSettings =
          armManagers
              .monitorManager()
              .diagnosticSettings()
              .define(
                  nameGenerator.nextName(ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH))
              .withResource(batchAccountId)
              .withLogAnalytics(logAnalyticsWorkspaceId)
              .withLog("ServiceLogs", 0) // retention is handled by the log analytics workspace
              .withLog("ServiceLog", 0)
              .withLog("AuditLog", 0);

      var postgresLogSettings =
          armManagers
              .monitorManager()
              .diagnosticSettings()
              .define(
                  nameGenerator.nextName(ResourceNameGenerator.MAX_DIAGNOSTIC_SETTING_NAME_LENGTH))
              .withResource(postgreSqlId)
              .withLogAnalytics(logAnalyticsWorkspaceId)
              .withLog("PostgreSQLLogs", 0) // retention is handled by the log analytics workspace
              .withLog("QueryStoreRuntimeStatistics", 0)
              .withLog("QueryStoreWaitStatistics", 0)
              .withMetric("AllMetrics", Duration.ofMinutes(1), 0);

      var appInsights =
          armManagers
              .applicationInsightsManager()
              .components()
              .define(
                  nameGenerator.nextName(
                      ResourceNameGenerator.MAX_APP_INSIGHTS_COMPONENT_NAME_LENGTH))
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup.name())
              .withKind("java")
              .withApplicationType(ApplicationType.OTHER)
              .withWorkspaceResourceId(logAnalyticsWorkspaceId);

      return deployment
          .withResourceWithPurpose(aks, ResourcePurpose.SHARED_RESOURCE)
          .withResourceWithPurpose(relay, ResourcePurpose.SHARED_RESOURCE)
          .withResourceWithPurpose(privateEndpoint, ResourcePurpose.SHARED_RESOURCE)
          .withResourceWithPurpose(storageAuditLogSettings, ResourcePurpose.SHARED_RESOURCE)
          .withResourceWithPurpose(batchLogSettings, ResourcePurpose.SHARED_RESOURCE)
          .withResourceWithPurpose(postgresLogSettings, ResourcePurpose.SHARED_RESOURCE)
          .withResourceWithPurpose(appInsights, ResourcePurpose.SHARED_RESOURCE);
    }

    private String getResourceId(List<DeployedResource> prerequisites, String resourceType) {
      return prerequisites.stream()
          .filter(deployedResource -> Objects.equals(deployedResource.resourceType(), resourceType))
          .findFirst()
          .get()
          .resourceId();
    }

    private Map<String, String> getDefaultParameters() {
      Map<String, String> defaultValues = new HashMap<>();
      defaultValues.put(ParametersNames.POSTGRES_DB_ADMIN.name(), "db_admin");
      defaultValues.put(ParametersNames.POSTGRES_DB_PASSWORD.name(), UUID.randomUUID().toString());
      defaultValues.put(ParametersNames.POSTGRES_SERVER_SKU.name(), "GP_Gen5_2");
      defaultValues.put(ParametersNames.VNET_ADDRESS_SPACE.name(), "10.1.0.0/27");
      defaultValues.put(Subnet.AKS_SUBNET.name(), "10.1.0.0/29");
      defaultValues.put(Subnet.BATCH_SUBNET.name(), "10.1.0.8/29");
      defaultValues.put(Subnet.POSTGRESQL_SUBNET.name(), "10.1.0.16/29");
      defaultValues.put(Subnet.COMPUTE_SUBNET.name(), "10.1.0.24/29");
      defaultValues.put(ParametersNames.AKS_NODE_COUNT.name(), String.valueOf(1));
      defaultValues.put(
          ParametersNames.AKS_MACHINE_TYPE.name(),
          ContainerServiceVMSizeTypes.STANDARD_A2_V2.toString());
      defaultValues.put(ParametersNames.AKS_AUTOSCALING_ENABLED.name(), String.valueOf(false));
      defaultValues.put(ParametersNames.AKS_AUTOSCALING_MIN.name(), String.valueOf(1));
      defaultValues.put(ParametersNames.AKS_AUTOSCALING_MAX.name(), String.valueOf(3));
      defaultValues.put(ParametersNames.AUDIT_LOG_RETENTION_DAYS.name(), "90");
      defaultValues.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS.name(),
          STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT);
      defaultValues.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS.name(),
          STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT);
      defaultValues.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS.name(),
          STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT);
      defaultValues.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS.name(),
          STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT);
      defaultValues.put(
          StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name(),
          STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT);
      defaultValues.put(ParametersNames.AKS_AAD_PROFILE_USER_GROUP_ID.name(), "00000000-0000-0000-0000-000000000000");

      return defaultValues;
    }
  }

  /**
   * This creates a data collection rule that should mimic closely how they are created via the
   * portal. Data collection rule can be used by virtual machines with the Azure Monitor Agent to
   * collect logs and metrics to the landing zone log analytics workspace.
   *
   * @param definitionContext
   * @param logAnalyticsWorkspaceId
   */
  private void createDataCollectionRule(
      DefinitionContext definitionContext, String logAnalyticsWorkspaceId) {
    // Arbitrary local names to connect things together within the rule
    final String DESTINATION_NAME = "lz_workspace";
    final String PERF_COUNTER_NAME = "VMInsightsPerfCounters";
    final String SYSLOG_NAME = "syslog";

    final List<String> COUNTER_SPECIFICS = List.of("\\VmInsights\\DetailedMetrics");

    armManagers
        .monitorManager()
        .serviceClient()
        .getDataCollectionRules()
        .createWithResponse(
            definitionContext.resourceGroup().name(),
            definitionContext
                .resourceNameGenerator()
                .nextName(ResourceNameGenerator.MAX_DATA_COLLECTION_RULE_NAME_LENGTH),
            new DataCollectionRuleResourceInner()
                .withDataSources(
                    new DataCollectionRuleDataSources()
                        .withPerformanceCounters(
                            List.of(
                                new PerfCounterDataSource()
                                    .withName(PERF_COUNTER_NAME)
                                    .withCounterSpecifiers(COUNTER_SPECIFICS)
                                    .withStreams(
                                        List.of(
                                            KnownPerfCounterDataSourceStreams
                                                .MICROSOFT_INSIGHTS_METRICS))
                                    .withSamplingFrequencyInSeconds(60)))
                        .withSyslog(
                            List.of(
                                new SyslogDataSource()
                                    .withName(SYSLOG_NAME)
                                    .withFacilityNames(
                                        List.of(KnownSyslogDataSourceFacilityNames.ASTERISK))
                                    .withLogLevels(List.of(KnownSyslogDataSourceLogLevels.ASTERISK))
                                    .withStreams(
                                        List.of(KnownSyslogDataSourceStreams.MICROSOFT_SYSLOG)))))
                .withDestinations(
                    new DataCollectionRuleDestinations()
                        .withLogAnalytics(
                            List.of(
                                new LogAnalyticsDestination()
                                    .withName(DESTINATION_NAME)
                                    .withWorkspaceResourceId(logAnalyticsWorkspaceId))))
                .withDataFlows(
                    List.of(
                        new DataFlow()
                            .withStreams(List.of(KnownDataFlowStreams.MICROSOFT_PERF))
                            .withDestinations(List.of(DESTINATION_NAME)),
                        new DataFlow()
                            .withStreams(List.of(KnownDataFlowStreams.MICROSOFT_SYSLOG))
                            .withDestinations(List.of(DESTINATION_NAME))))
                .withLocation(definitionContext.resourceGroup().region().name())
                .withTags(
                    Map.of(
                        LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                        definitionContext.landingZoneId(),
                        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                        ResourcePurpose.SHARED_RESOURCE.toString())),
            Context.NONE);
  }

  /**
   * Build list of Cors rules for storage account
   *
   * @param parametersResolver
   * @return List of Cors rules
   */
  private ArrayList<CorsRule> buildStorageAccountBlobServiceCorsRules(
      ParametersResolver parametersResolver) {
    ArrayList<CorsRule> corsRules = new ArrayList<>();

    var rule = new CorsRule();
    var origins =
        Arrays.stream(
                parametersResolver
                    .getValue(
                        StorageAccountBlobCorsParametersNames
                            .STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS
                            .name())
                    .split(","))
            .toList();
    rule.withAllowedOrigins(origins);

    var methods =
        ParametersExtractor.extractAllowedMethods(parametersResolver).stream()
            .map(CorsRuleAllowedMethodsItem::fromString)
            .toList();
    rule.withAllowedMethods(methods);

    var headers =
        Arrays.stream(
                parametersResolver
                    .getValue(
                        StorageAccountBlobCorsParametersNames
                            .STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS
                            .name())
                    .split(","))
            .toList();
    rule.withAllowedHeaders(headers);

    List<String> expHeaders =
        Arrays.stream(
                parametersResolver
                    .getValue(
                        StorageAccountBlobCorsParametersNames
                            .STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS
                            .name())
                    .split(","))
            .toList();
    rule.withExposedHeaders(expHeaders);

    rule =
        rule.withMaxAgeInSeconds(
            Integer.parseInt(
                parametersResolver.getValue(
                    StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE
                        .name())));

    corsRules.add(rule);
    return corsRules;
  }

  /**
   * Setup Cors configuration for a storage account
   *
   * @param azureResourceManager
   * @param resourceGroupName
   * @param storageAccountName
   * @param corsRules
   */
  private void setupCorsForStorageAccountBlobService(
      AzureResourceManager azureResourceManager,
      String resourceGroupName,
      String storageAccountName,
      List<CorsRule> corsRules) {
    azureResourceManager
        .storageBlobServices()
        .define("blobCorsConfiguration")
        .withExistingStorageAccount(resourceGroupName, storageAccountName)
        .withCORSRules(corsRules)
        .create();
  }
}
