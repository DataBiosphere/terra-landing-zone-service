package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactoryListProviderImpl;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import bio.terra.landingzone.library.landingzones.management.quotas.QuotaProvider;
import bio.terra.landingzone.library.landingzones.management.quotas.ResourceQuota;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.UserAgentPolicy;
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
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * High level component to deploy and list deployment definitions, and listing resources by purpose.
 */
public class LandingZoneManager {
  private static final ClientLogger logger = new ClientLogger(LandingZoneManager.class);
  private final ResourceGroup resourceGroup;
  private final ResourcesReader resourcesReader;
  private final QuotaProvider quotaProvider;

  private final ResourcesDeleteManager resourcesDeleteManager;

  LandingZoneManager(
      ResourceGroup resourceGroup,
      ResourcesReader resourcesReader,
      QuotaProvider quotaProvider,
      ResourcesDeleteManager resourcesDeleteManager) {
    this.resourceGroup = resourceGroup;
    this.resourcesReader = resourcesReader;
    this.quotaProvider = quotaProvider;
    this.resourcesDeleteManager = resourcesDeleteManager;
  }

  public static LandingZoneManager createLandingZoneManager(
      TokenCredential credential,
      AzureProfile profile,
      String resourceGroupName,
      String azureCustomerUsageAttribute) {

    Objects.requireNonNull(credential, "credential can't be null");
    Objects.requireNonNull(profile, "profile can't be null");
    if (StringUtils.isBlank(resourceGroupName)) {
      throw logger.logExceptionAsError(
          new IllegalArgumentException("Resource group name can't be blank or null"));
    }

    ArmManagers armManagers = createArmManagers(credential, profile, azureCustomerUsageAttribute);
    ResourceGroup resourceGroup =
        armManagers.azureResourceManager().resourceGroups().getByName(resourceGroupName);
    DeleteRulesVerifier deleteRulesVerifier = new DeleteRulesVerifier(armManagers);
    return new LandingZoneManager(
        resourceGroup,
        new ResourcesReaderImpl(armManagers.azureResourceManager(), resourceGroup),
        new QuotaProvider(armManagers),
        new ResourcesDeleteManager(armManagers, deleteRulesVerifier));
  }

  public static ArmManagers createArmManagers(
      TokenCredential credential, AzureProfile profile, String azureCustomerUsageAttribute) {
    final Optional<UserAgentPolicy> resourceUsagePolicy =
        getUserAgentPolicy(azureCustomerUsageAttribute);
    AzureResourceManager.Configurable azureResourceManagerConfigurable =
        AzureResourceManager.configure();
    resourceUsagePolicy.ifPresent(azureResourceManagerConfigurable::withPolicy);
    AzureResourceManager azureResourceManager =
        azureResourceManagerConfigurable
            .authenticate(credential, profile)
            .withSubscription(profile.getSubscriptionId());

    RelayManager.Configurable relayManagerConfigurable = RelayManager.configure();
    resourceUsagePolicy.ifPresent(relayManagerConfigurable::withPolicy);
    RelayManager relayManager = relayManagerConfigurable.authenticate(credential, profile);

    BatchManager.Configurable batchManagerConfigurable = BatchManager.configure();
    resourceUsagePolicy.ifPresent(batchManagerConfigurable::withPolicy);
    BatchManager batchManager = batchManagerConfigurable.authenticate(credential, profile);

    PostgreSqlManager.Configurable postgreSqlManagerConfigurable = PostgreSqlManager.configure();
    resourceUsagePolicy.ifPresent(postgreSqlManagerConfigurable::withPolicy);
    PostgreSqlManager postgreSqlManager =
        postgreSqlManagerConfigurable.authenticate(credential, profile);

    LogAnalyticsManager.Configurable logAnalyticsManagerConfigurable =
        LogAnalyticsManager.configure();
    resourceUsagePolicy.ifPresent(logAnalyticsManagerConfigurable::withPolicy);
    LogAnalyticsManager logAnalyticsManager =
        logAnalyticsManagerConfigurable.authenticate(credential, profile);

    MonitorManager.Configurable monitorManagerConfigurable = MonitorManager.configure();
    resourceUsagePolicy.ifPresent(monitorManagerConfigurable::withPolicy);
    MonitorManager monitorManager = monitorManagerConfigurable.authenticate(credential, profile);

    ApplicationInsightsManager.Configurable applicationInsightsManagerConfigurable =
        ApplicationInsightsManager.configure();
    resourceUsagePolicy.ifPresent(applicationInsightsManagerConfigurable::withPolicy);
    ApplicationInsightsManager applicationInsightsManager =
        applicationInsightsManagerConfigurable.authenticate(credential, profile);

    SecurityInsightsManager.Configurable securityInsightsManagerConfigurable =
        SecurityInsightsManager.configure();
    resourceUsagePolicy.ifPresent(securityInsightsManagerConfigurable::withPolicy);
    SecurityInsightsManager securityInsightsManager =
        securityInsightsManagerConfigurable.authenticate(credential, profile);

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

  public List<String> deleteResources(String landingZoneId) throws LandingZoneRuleDeleteException {
    return resourcesDeleteManager
        .deleteLandingZoneResources(landingZoneId, resourceGroup.name())
        .stream()
        .map(HasId::id)
        .toList();
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

  public Region getLandingZoneRegion() {
    return resourceGroup.region();
  }

  private static Optional<UserAgentPolicy> getUserAgentPolicy(String azureCustomerUsageAttribute) {
    return StringUtils.isNotEmpty(azureCustomerUsageAttribute)
        ? Optional.of(new UserAgentPolicy(azureCustomerUsageAttribute))
        : Optional.empty();
  }
}
