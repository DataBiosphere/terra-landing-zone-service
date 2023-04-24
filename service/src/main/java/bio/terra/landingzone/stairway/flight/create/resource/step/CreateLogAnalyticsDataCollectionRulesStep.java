package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import com.azure.core.util.Context;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateLogAnalyticsDataCollectionRulesStep extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateLogAnalyticsDataCollectionRulesStep.class);

  public CreateLogAnalyticsDataCollectionRulesStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return StepResult.getStepResultSuccess();
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    final String DESTINATION_NAME = "lz_workspace";
    final String PERF_COUNTER_NAME = "VMInsightsPerfCounters";
    final String SYSLOG_NAME = "syslog";

    final List<String> COUNTER_SPECIFICS = List.of("\\VmInsights\\DetailedMetrics");
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);
    var dataCollectionRulesName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_DATA_COLLECTION_RULE_NAME_LENGTH);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    var dataCollectionRules =
        armManagers
            .monitorManager()
            .serviceClient()
            .getDataCollectionRules()
            .createWithResponse(
                resourceGroup.name(),
                dataCollectionRulesName,
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
                                        .withLogLevels(
                                            List.of(KnownSyslogDataSourceLogLevels.ASTERISK))
                                        .withStreams(
                                            List.of(
                                                KnownSyslogDataSourceStreams.MICROSOFT_SYSLOG)))))
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
                    .withLocation(resourceGroup.region().name())
                    .withTags(
                        Map.of(
                            LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                            landingZoneId.toString(),
                            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                            ResourcePurpose.SHARED_RESOURCE.toString())),
                Context.NONE);
    logger.info(
        RESOURCE_CREATED,
        getResourceType(),
        dataCollectionRules.getValue().id(),
        resourceGroup.name());
  }

  @Override
  protected String getResourceType() {
    return "LogAnalyticsDataCollectionRules";
  }
}
