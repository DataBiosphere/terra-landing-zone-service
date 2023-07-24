package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.util.Context;
import com.azure.resourcemanager.monitor.fluent.models.DataCollectionRuleAssociationProxyOnlyResourceInner;
import com.azure.resourcemanager.monitor.fluent.models.DataCollectionRuleResourceInner;
import com.azure.resourcemanager.monitor.models.DataCollectionRuleDataSources;
import com.azure.resourcemanager.monitor.models.DataCollectionRuleDestinations;
import com.azure.resourcemanager.monitor.models.DataFlow;
import com.azure.resourcemanager.monitor.models.ExtensionDataSource;
import com.azure.resourcemanager.monitor.models.KnownDataFlowStreams;
import com.azure.resourcemanager.monitor.models.KnownExtensionDataSourceStreams;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceFacilityNames;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceLogLevels;
import com.azure.resourcemanager.monitor.models.KnownSyslogDataSourceStreams;
import com.azure.resourcemanager.monitor.models.LogAnalyticsDestination;
import com.azure.resourcemanager.monitor.models.SyslogDataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * This step configures container insights cost optimization data collection rule. Official
 * documentation
 * https://learn.microsoft.com/en-us/azure/azure-monitor/containers/container-insights-cost-config?tabs=create-CLI
 *
 * <p>There are 3 main parameters which can be set up to tune optimization: interval - This value
 * determines how often the agent collects data. namespaceFilteringMode - If it is set to "Include"
 * then only data from namespaces which are set in the namespaces' field would be collected.
 * namespaces - Array of comma separated Kubernetes namespaces for which inventory and perf data
 * will be included or excluded based on the namespaceFilteringMode. This parameter is not included
 * into current configuration since namespaceFilteringMode is set to Off. But can be added later by
 * adding 'namespaces' field into DataCollectionSettings.
 */

/**
 * **********************************!!! WARNING !!!**********************************
 *
 * <p>This step is currently excluded from LZ flight because it breaks K8s monitoring. The cost
 * optimization settings requires following parameter 'useAADAuth' set to true, but at the same time
 * setting this value breaks K8s monitoring. This step is temporarily disabled until we find
 * workaround or proper resolution.
 */
public class CreateAksCostOptimizationDataCollectionRulesStep extends BaseResourceCreateStep {
  public static final String AKS_COST_OPTIMIZATION_DATA_COLLECTION_RULE_ID =
      "AKS_COST_OPTIMIZATION_DATA_COLLECTION_RULE_ID";
  public static final String DATA_COLLECTION_INTERVAL = "30m";
  public static final String DATA_COLLECTION_NAMESPACE_FILTERING_MODE = "Off";
  private static final Logger logger =
      LoggerFactory.getLogger(CreateAksCostOptimizationDataCollectionRulesStep.class);

  private static final ExtensionSettings defaultExtensionSettings =
      new ExtensionSettings(
          new ExtensionSettings.DataCollectionSettings(
              DATA_COLLECTION_INTERVAL, DATA_COLLECTION_NAMESPACE_FILTERING_MODE));

  static class ExtensionSettings {
    static class DataCollectionSettings {
      private final String interval;
      private final String namespaceFilteringMode;

      public DataCollectionSettings(String interval, String namespaceFilteringMode) {
        this.interval = interval;
        this.namespaceFilteringMode = namespaceFilteringMode;
      }

      public String getInterval() {
        return interval;
      }

      public String getNamespaceFilteringMode() {
        return namespaceFilteringMode;
      }
    }

    private final DataCollectionSettings dataCollectionSettings;

    public ExtensionSettings(DataCollectionSettings dataCollectionSettings) {
      this.dataCollectionSettings = dataCollectionSettings;
    }

    public DataCollectionSettings getDataCollectionSettings() {
      return dataCollectionSettings;
    }
  }

  public CreateAksCostOptimizationDataCollectionRulesStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider) {
    super(armManagers, parametersResolver, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    var aksId = getParameterOrThrow(context.getWorkingMap(), CreateAksStep.AKS_ID, String.class);

    var dataCollectionRuleId = createRule(landingZoneId, logAnalyticsWorkspaceId, context);
    // associate rule with aks resource.
    createRuleAssociation(aksId, dataCollectionRuleId);
    logger.info(RESOURCE_CREATED, getResourceType(), dataCollectionRuleId, getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    // do nothing
  }

  @Override
  protected String getResourceType() {
    return "AksCostOptimizationDataCollectionRule";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.empty();
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_DATA_COLLECTION_RULE_NAME_LENGTH));
  }

  private String createRule(
      UUID landingZoneId, String logAnalyticsWorkspaceId, FlightContext context) {
    // if this is created via portal name of the rule has the following format by default:
    // "MSCI-k8sRegion-k8sName"
    // but this behavior looks like Azure portal limitation since this rule is created implicitly
    // and user doesn't set it.
    var dataCollectionRuleName = resourceNameProvider.getName(getResourceType());
    try {
      var dataCollectionRule =
          armManagers
              .monitorManager()
              .serviceClient()
              .getDataCollectionRules()
              .createWithResponse(
                  getMRGName(context),
                  dataCollectionRuleName,
                  configureDataCollectionRule(
                      landingZoneId, logAnalyticsWorkspaceId, getMRGRegionName(context)),
                  Context.NONE);
      context
          .getWorkingMap()
          .put(AKS_COST_OPTIMIZATION_DATA_COLLECTION_RULE_ID, dataCollectionRule.getValue().id());
      return dataCollectionRule.getValue().id();
    } catch (ManagementException e) {
      if (e.getResponse() != null
          && HttpStatus.CONFLICT.value() == e.getResponse().getStatusCode()) {
        return armManagers
            .monitorManager()
            .serviceClient()
            .getDataCollectionRules()
            .getByResourceGroup(getMRGName(context), dataCollectionRuleName)
            .id();
      } else {
        throw e;
      }
    }
  }

  private void createRuleAssociation(String aksId, String dataCollectionRuleId) {
    final String RULE_ASSOCIATION_DESCRIPTION =
        "Association of data collection rule. Deleting this association will break the data collection for this AKS Cluster.";
    var ruleAssociation =
        armManagers
            .monitorManager()
            .serviceClient()
            .getDataCollectionRuleAssociations()
            .createWithResponse(
                aksId,
                "ContainerInsightsExtension",
                new DataCollectionRuleAssociationProxyOnlyResourceInner()
                    .withDataCollectionRuleId(dataCollectionRuleId)
                    .withDescription(RULE_ASSOCIATION_DESCRIPTION),
                Context.NONE);
    logger.info(
        "Association between rule with id={} and resource with id={} created. Association id={}",
        dataCollectionRuleId,
        aksId,
        ruleAssociation.getValue().id());
  }

  private DataCollectionRuleResourceInner configureDataCollectionRule(
      UUID landingZoneId, String logAnalyticsWorkspaceId, String regionName) {
    final String DESTINATION_NAME = "lz_workspace";
    return new DataCollectionRuleResourceInner()
        .withDataSources(
            new DataCollectionRuleDataSources()
                .withExtensions(
                    List.of(
                        new ExtensionDataSource()
                            .withName("ContainerInsightsExtension")
                            .withStreams(
                                List.of(KnownExtensionDataSourceStreams.MICROSOFT_INSIGHTS_METRICS))
                            .withExtensionName("ContainerInsights")
                            .withExtensionSettings(defaultExtensionSettings)))
                .withSyslog(
                    List.of(
                        new SyslogDataSource()
                            .withName("sysLogsDataSource")
                            .withFacilityNames(getSyslogFacilityNames())
                            .withLogLevels(getSyslogLogLevels())
                            .withStreams(List.of(KnownSyslogDataSourceStreams.MICROSOFT_SYSLOG)))))
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
                    .withStreams(
                        List.of(
                            KnownDataFlowStreams.MICROSOFT_INSIGHTS_METRICS,
                            KnownDataFlowStreams.MICROSOFT_SYSLOG))
                    .withDestinations(List.of(DESTINATION_NAME))))
        .withLocation(regionName)
        .withTags(
            Map.of(
                LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                landingZoneId.toString(),
                LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                ResourcePurpose.SHARED_RESOURCE.toString()));
  }

  private List<KnownSyslogDataSourceLogLevels> getSyslogLogLevels() {
    // attempt to use just ASTERISK for everything leads to an error on UI
    // which shows that logLevel is not specified
    return KnownSyslogDataSourceLogLevels.values().stream()
        .filter(v -> !v.equals(KnownSyslogDataSourceLogLevels.ASTERISK))
        .toList();
  }

  private List<KnownSyslogDataSourceFacilityNames> getSyslogFacilityNames() {
    // when such rule is created using Azure portal this parameter is also set explicitly
    // so, applying the same behavior here as well
    return KnownSyslogDataSourceFacilityNames.values().stream()
        .filter(v -> !v.equals(KnownSyslogDataSourceFacilityNames.ASTERISK))
        .toList();
  }
}
