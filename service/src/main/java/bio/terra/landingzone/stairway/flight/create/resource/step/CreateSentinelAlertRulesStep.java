package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.landingzone.stairway.flight.utils.AlertRulesHelper;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.securityinsights.models.AlertSeverity;
import com.azure.resourcemanager.securityinsights.models.MLBehaviorAnalyticsAlertRule;
import com.azure.resourcemanager.securityinsights.models.ScheduledAlertRule;
import com.azure.resourcemanager.securityinsights.models.TriggerOperator;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSentinelAlertRulesStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateSentinelAlertRulesStep.class);
  private final AlertRulesHelper alertRulesHelper;
  private final LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration;

  public CreateSentinelAlertRulesStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider,
      AlertRulesHelper alertRuleAdapter,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    super(armManagers, parametersResolver, resourceNameProvider);
    this.alertRulesHelper = alertRuleAdapter;
    this.landingZoneProtectedDataConfiguration = landingZoneProtectedDataConfiguration;
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var logAnalyticsWorkspace =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            LandingZoneResource.class);

    var logAnalyticsWorkspaceResourceName = logAnalyticsWorkspace.resourceName();
    if (logAnalyticsWorkspaceResourceName.isEmpty()) {
      throw new MissingRequiredFieldsException("LogAnalyticsWorkspace resource name is not set.");
    }

    // create the alert rules -- note we do not handle 409s for these rules as the underlying API
    // call is an upsert rather than a create and should succeed in the event of a step retry
    logger.info("Creating sentinel alert rules...");

    var mrgName = getMRGName(context);
    var lawName = logAnalyticsWorkspaceResourceName.get();

    createScheduledAlertRules(mrgName, lawName);
    createMlAlertRules(mrgName, lawName);
    createNrtAlertRules(mrgName, lawName);
    createCustomRules(mrgName, lawName);
  }

  @Override
  protected void deleteResource(String resourceId) {
    // noop
  }

  @Override
  protected String getResourceType() {
    return "SentinelAlertRules";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.empty();
  }

  private void createScheduledAlertRules(String mrgName, String workspaceName) {
    landingZoneProtectedDataConfiguration
        .getSentinelScheduledAlertRuleTemplateIds()
        .forEach(
            ruleTemplateId -> {
              var rule =
                  alertRulesHelper.buildScheduledAlertRuleFromTemplate(
                      mrgName, workspaceName, ruleTemplateId);
              alertRulesHelper.createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
            });
  }

  private void createMlAlertRules(String mrgName, String workspaceName) {
    landingZoneProtectedDataConfiguration
        .getSentinelMlRuleTemplateIds()
        .forEach(
            ruleTemplateId -> {
              var rule =
                  new MLBehaviorAnalyticsAlertRule()
                      .withAlertRuleTemplateName(ruleTemplateId)
                      .withEnabled(true);
              alertRulesHelper.createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
            });
  }

  private void createNrtAlertRules(String mrgName, String workspaceName) {
    landingZoneProtectedDataConfiguration
        .getSentinelNrtRuleTemplateIds()
        .forEach(
            ruleTemplateId -> {
              var rule =
                  alertRulesHelper.buildNrtAlertRuleFromTemplate(
                      mrgName, workspaceName, ruleTemplateId);
              alertRulesHelper.createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
            });
  }

  private void createCustomRules(String mrgName, String workspaceName) {
    var fileAccessAttemptsRule =
        new ScheduledAlertRule()
            .withDisplayName("File access attempts by unauthorized user accounts")
            .withQuery(
                """
                                      StorageBlobLogs\s
                                      | where StatusCode in (403)
                                      | extend CallerIpAddress = tostring(split(CallerIpAddress, ":")[0]),
                                               Identity = coalesce(parse_urlquery(Uri)["Query Parameters"]["rscd"], AuthenticationHash, AuthenticationType)
                                      | summarize
                                          Attempts = count(), TimeStart = min(TimeGenerated), TimeEnd = max(TimeGenerated)
                                          by AccountName, CallerIpAddress, Identity, bin(TimeGenerated, 10m)
                                      | where Attempts > 10
                                      | project TimeStart, TimeEnd, Attempts, CallerIpAddress, AccountName, Identity
                                    """)
            .withSuppressionEnabled(false)
            .withSuppressionDuration(Duration.parse("PT1H"))
            .withQueryPeriod(Duration.ofDays(1))
            .withQueryFrequency(Duration.ofDays(1))
            .withEnabled(true)
            .withSeverity(AlertSeverity.INFORMATIONAL)
            .withTriggerOperator(TriggerOperator.GREATER_THAN)
            .withTriggerThreshold(0);
    alertRulesHelper.createAlertRule(
        fileAccessAttemptsRule, "UnauthorizedFileAccessAttempts", mrgName, workspaceName);
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of();
  }
}
