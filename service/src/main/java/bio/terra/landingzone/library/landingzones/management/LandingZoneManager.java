package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionContext;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactoryListProviderImpl;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionProvider;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionProviderImpl;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployments;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeploymentsImpl;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import bio.terra.landingzone.library.landingzones.management.quotas.QuotaProvider;
import bio.terra.landingzone.library.landingzones.management.quotas.ResourceQuota;
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.applicationinsights.ApplicationInsightsManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.loganalytics.LogAnalyticsManager;
import com.azure.resourcemanager.monitor.MonitorManager;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.relay.RelayManager;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.securityinsights.SecurityInsightsManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

/**
 * High level component to deploy and list deployment definitions, and listing resources by purpose.
 */
public class LandingZoneManager {
  private static final ClientLogger logger = new ClientLogger(LandingZoneManager.class);
  private final LandingZoneDefinitionProvider landingZoneDefinitionProvider;
  private final LandingZoneDeployments landingZoneDeployments;
  private final AzureResourceManager resourceManager;
  private final ResourceGroup resourceGroup;
  private final ResourcesReader resourcesReader;
  private final QuotaProvider quotaProvider;

  private final ResourcesDeleteManager resourcesDeleteManager;

  LandingZoneManager(
      LandingZoneDefinitionProvider landingZoneDefinitionProvider,
      LandingZoneDeployments landingZoneDeployments,
      AzureResourceManager resourceManager,
      ResourceGroup resourceGroup,
      ResourcesReader resourcesReader,
      QuotaProvider quotaProvider,
      ResourcesDeleteManager resourcesDeleteManager) {
    this.landingZoneDefinitionProvider = landingZoneDefinitionProvider;
    this.landingZoneDeployments = landingZoneDeployments;
    this.resourceManager = resourceManager;
    this.resourceGroup = resourceGroup;
    this.resourcesReader = resourcesReader;
    this.quotaProvider = quotaProvider;
    this.resourcesDeleteManager = resourcesDeleteManager;
  }

  public static LandingZoneManager createLandingZoneManager(
      TokenCredential credential, AzureProfile profile, String resourceGroupName) {

    Objects.requireNonNull(credential, "credential can't be null");
    Objects.requireNonNull(profile, "profile can't be null");
    if (StringUtils.isBlank(resourceGroupName)) {
      throw logger.logExceptionAsError(
          new IllegalArgumentException("Resource group name can't be blank or null"));
    }

    ArmManagers armManagers = createArmManagers(credential, profile);
    ResourceGroup resourceGroup =
        armManagers.azureResourceManager().resourceGroups().getByName(resourceGroupName);
    DeleteRulesVerifier deleteRulesVerifier = new DeleteRulesVerifier(armManagers);
    return new LandingZoneManager(
        new LandingZoneDefinitionProviderImpl(armManagers),
        new LandingZoneDeploymentsImpl(),
        armManagers.azureResourceManager(),
        resourceGroup,
        new ResourcesReaderImpl(armManagers.azureResourceManager(), resourceGroup),
        new QuotaProvider(armManagers),
        new ResourcesDeleteManager(armManagers, deleteRulesVerifier));
  }

  public static ArmManagers createArmManagers(TokenCredential credential, AzureProfile profile) {
    AzureResourceManager azureResourceManager =
        AzureResourceManager.authenticate(credential, profile)
            .withSubscription(profile.getSubscriptionId());
    RelayManager relayManager = RelayManager.authenticate(credential, profile);
    BatchManager batchManager = BatchManager.authenticate(credential, profile);
    PostgreSqlManager postgreSqlManager = PostgreSqlManager.authenticate(credential, profile);
    LogAnalyticsManager logAnalyticsManager = LogAnalyticsManager.authenticate(credential, profile);
    MonitorManager monitorManager = MonitorManager.authenticate(credential, profile);
    ApplicationInsightsManager applicationInsightsManager =
        ApplicationInsightsManager.authenticate(credential, profile);
    SecurityInsightsManager securityInsightsManager =
        SecurityInsightsManager.authenticate(credential, profile);

    return new ArmManagers(
        azureResourceManager,
        relayManager,
        batchManager,
        postgreSqlManager,
        logAnalyticsManager,
        monitorManager,
        applicationInsightsManager,
        securityInsightsManager);
  }

  public static List<FactoryDefinitionInfo> listDefinitionFactories() {
    var landingZoneDefinitionListProvider = new LandingZoneDefinitionFactoryListProviderImpl();
    return landingZoneDefinitionListProvider.listFactories();
  }

  public List<DeployedResource> deployLandingZone(
      String landingZoneId,
      String className,
      DefinitionVersion version,
      Map<String, String> parameters) {

    return deployLandingZoneAsync(landingZoneId, className, version, parameters)
        .collectList()
        .block();
  }

  public Flux<DeployedResource> deployLandingZoneAsync(
      String landingZoneId,
      String className,
      DefinitionVersion version,
      Map<String, String> parameters) {

    Class<? extends LandingZoneDefinitionFactory> factory = getFactoryFromClassName(className);
    Objects.requireNonNull(factory, "Factory information can't be null");
    Objects.requireNonNull(version, "Factory version can't be null");
    if (StringUtils.isBlank(landingZoneId)) {
      throw logger.logExceptionAsError(
          new IllegalArgumentException("Landing Zone ID can't be null or blank"));
    }

    return landingZoneDefinitionProvider
        .createDefinitionFactory(factory)
        .create(version)
        .definition(createNewDefinitionContext(landingZoneId, parameters))
        .deployAsync();
  }

  public List<String> deleteResources(String landingZoneId) throws LandingZoneRuleDeleteException {
    return resourcesDeleteManager
        .deleteLandingZoneResources(landingZoneId, resourceGroup.name())
        .stream()
        .map(HasId::id)
        .toList();
  }

  private Class<? extends LandingZoneDefinitionFactory> getFactoryFromClassName(String className) {

    return new LandingZoneDefinitionFactoryListProviderImpl()
        .listFactoriesClasses().stream()
            .filter(f -> f.getSimpleName().equals(className))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Invalid factory definition name"));
  }

  private DefinitionContext createNewDefinitionContext(
      String landingZoneId, Map<String, String> parameters) {
    return new DefinitionContext(
        landingZoneId,
        landingZoneDeployments.define(landingZoneId),
        resourceGroup,
        new ResourceNameGenerator(landingZoneId),
        parameters);
  }

  /**
   * Returns quota information for a landing zone resource.
   *
   * @param landingZoneId landing zone id.
   * @param resourceId resource id.
   * @return quota information.
   */
  public ResourceQuota resourceQuota(String landingZoneId, String resourceId) {
    if (StringUtils.isBlank(resourceId)) {
      throw new IllegalArgumentException("Resource id is required.");
    }
    if (StringUtils.isBlank(landingZoneId)) {
      throw new IllegalArgumentException("Landing zone id is required.");
    }

    var deployedResource =
        resourcesReader.listAllResources(landingZoneId).stream()
            .filter(r -> r.resourceId().equalsIgnoreCase(resourceId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "The resource was not found in the landing zone. Please make sure that the landing zone deployment is complete."));

    return quotaProvider.resourceQuota(deployedResource.resourceId());
  }

  public ResourcesReader reader() {
    return resourcesReader;
  }

  public LandingZoneDeployments deployments() {
    return landingZoneDeployments;
  }

  public LandingZoneDefinitionProvider provider() {
    return landingZoneDefinitionProvider;
  }

  public Region getLandingZoneRegion() {
    return resourceGroup.region();
  }
}
