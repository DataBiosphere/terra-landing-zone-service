package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
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
import org.apache.commons.lang3.StringUtils;
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
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);

    final String DESTINATION_NAME = "lz_workspace";
    final String PERF_COUNTER_NAME = "VMInsightsPerfCounters";
    final String SYSLOG_NAME = "syslog";

    final List<String> COUNTER_SPECIFICS = List.of("\\VmInsights\\DetailedMetrics");
    var dataCollectionRulesName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_DATA_COLLECTION_RULE_NAME_LENGTH);
    try {
      var logAnalyticsWorkspaceId =
          getParameterOrThrow(
              context.getWorkingMap(),
              CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
              String.class);
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
                  .withLocation(resourceGroup.region().name())
                  .withTags(
                      Map.of(
                          LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                          landingZoneId.toString(),
                          LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                          ResourcePurpose.SHARED_RESOURCE.toString())),
              Context.NONE);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(
            RESOURCE_ALREADY_EXISTS,
            "Data collection rules",
            dataCollectionRulesName,
            resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "data collection rules", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    return StepResult.getStepResultSuccess();
  }
}
