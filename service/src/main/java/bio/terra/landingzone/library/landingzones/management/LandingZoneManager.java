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
import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.relay.RelayManager;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

  private final ResourcesDeleteManager resourcesDeleteManager;

  private LandingZoneManager(
      LandingZoneDefinitionProvider landingZoneDefinitionProvider,
      LandingZoneDeployments landingZoneDeployments,
      AzureResourceManager resourceManager,
      ResourceGroup resourceGroup,
      ResourcesReader resourcesReader,
      ResourcesDeleteManager resourcesDeleteManager) {
    this.landingZoneDefinitionProvider = landingZoneDefinitionProvider;
    this.landingZoneDeployments = landingZoneDeployments;
    this.resourceManager = resourceManager;
    this.resourceGroup = resourceGroup;
    this.resourcesReader = resourcesReader;
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
        new ResourcesDeleteManager(armManagers, deleteRulesVerifier));
  }

  private static ArmManagers createArmManagers(TokenCredential credential, AzureProfile profile) {
    AzureResourceManager azureResourceManager =
        AzureResourceManager.authenticate(credential, profile)
            .withSubscription(profile.getSubscriptionId());
    RelayManager relayManager = RelayManager.authenticate(credential, profile);
    BatchManager batchManager = BatchManager.authenticate(credential, profile);
    PostgreSqlManager postgreSqlManager = PostgreSqlManager.authenticate(credential, profile);

    return new ArmManagers(azureResourceManager, relayManager, batchManager, postgreSqlManager);
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
        .collect(Collectors.toList());
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

  public ResourcesReader reader() {
    return resourcesReader;
  }

  public LandingZoneDeployments deployments() {
    return landingZoneDeployments;
  }

  public LandingZoneDefinitionProvider provider() {
    return landingZoneDefinitionProvider;
  }
}
