package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
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
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateLogAnalyticsDataCollectionRulesStep extends BaseResourceCreateStep {
  public static final String DATA_COLLECTION_RULE_NAME = "DATA_COLLECTION_RULES_NAME";
  private static final Logger logger =
      LoggerFactory.getLogger(CreateLogAnalyticsDataCollectionRulesStep.class);

  public CreateLogAnalyticsDataCollectionRulesStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator) {
    super(armManagers, parametersResolver, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    var dataCollectionRuleName =
        context.getWorkingMap().get(DATA_COLLECTION_RULE_NAME, String.class);
    try {
      if (dataCollectionRuleName != null) {
        armManagers
            .monitorManager()
            .serviceClient()
            .getDataCollectionRules()
            .delete(getMRGName(context), dataCollectionRuleName);
        logger.info("{} resource with id={} deleted.", getResourceType(), dataCollectionRuleName);
      }
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error(
            "Data collection rule doesn't exist or has been already deleted. Id={}",
            dataCollectionRuleName);
        return StepResult.getStepResultSuccess();
      }
      logger.error("Failed attempt to delete data collection rule. Id={}", dataCollectionRuleName);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
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
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    var dataCollectionRulesName =
        resourceNameGenerator.nextName(ResourceNameGenerator.MAX_DATA_COLLECTION_RULE_NAME_LENGTH);
    var dataCollectionRules =
        armManagers
            .monitorManager()
            .serviceClient()
            .getDataCollectionRules()
            .createWithResponse(
                getMRGName(context),
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
                    .withLocation(getMRGRegionName(context))
                    .withTags(
                        Map.of(
                            LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                            landingZoneId.toString(),
                            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                            ResourcePurpose.SHARED_RESOURCE.toString())),
                Context.NONE);
    context.getWorkingMap().put(DATA_COLLECTION_RULE_NAME, dataCollectionRulesName);
    logger.info(
        RESOURCE_CREATED,
        getResourceType(),
        dataCollectionRules.getValue().id(),
        getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    // do nothing
  }

  @Override
  protected String getResourceType() {
    return "LogAnalyticsDataCollectionRules";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.empty();
  }
}
