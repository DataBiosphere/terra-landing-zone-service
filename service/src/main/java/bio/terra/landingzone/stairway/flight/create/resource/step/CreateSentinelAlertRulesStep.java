package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.securityinsights.fluent.models.AlertRuleInner;
import com.azure.resourcemanager.securityinsights.implementation.AlertRuleTemplateImpl;
import com.azure.resourcemanager.securityinsights.models.MLBehaviorAnalyticsAlertRule;
import com.azure.resourcemanager.securityinsights.models.NrtAlertRule;
import com.azure.resourcemanager.securityinsights.models.NrtAlertRuleTemplate;
import com.azure.resourcemanager.securityinsights.models.ScheduledAlertRule;
import com.azure.resourcemanager.securityinsights.models.ScheduledAlertRuleTemplate;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSentinelAlertRulesStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateSentinelAlertRulesStep.class);
  private LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration;

  public CreateSentinelAlertRulesStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    super(armManagers, parametersResolver, resourceNameGenerator);
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

    logger.info("Creating sentinel scheduled alert rules...");
    var mrgName = getMRGName(context);
    var lawName = logAnalyticsWorkspaceResourceName.get();
    createScheduledAlertRules(mrgName, lawName);
    createMlAlertRules(mrgName, lawName);
    createNrtAlertRules(mrgName, lawName);
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
              logger.info("Creating alert rule from source template {}...", ruleTemplateId);
              var rule =
                  buildScheduledAlertRuleFromTemplate(mrgName, workspaceName, ruleTemplateId);
              createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
            });
  }

  private void createMlAlertRules(String mrgName, String workspaceName) {
    var mlRules = landingZoneProtectedDataConfiguration.getSentinelMlRuleTemplateIds();
    mlRules.forEach(
        ruleTemplateId -> {
          logger.info("Creating alert rule from source template {}...", ruleTemplateId);
          var rule =
              new MLBehaviorAnalyticsAlertRule()
                  .withAlertRuleTemplateName(ruleTemplateId)
                  .withEnabled(true);
          createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
        });
  }

  private void createNrtAlertRules(String mrgName, String workspaceName) {
    var ruleIds = landingZoneProtectedDataConfiguration.getSentinelNrtRuleTemplateIds();
    ruleIds.forEach(
        ruleTemplateId -> {
          logger.info("Creating alert rule from source template {}...", ruleTemplateId);
          var rule = buildNrtAlertRuleFromTemplate(mrgName, workspaceName, ruleTemplateId);
          createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
        });
  }

  private void createAlertRule(
      AlertRuleInner alertRule, String templateName, String mrgName, String workspaceName) {
    var sim = armManagers.securityInsightsManager();
    try {
      sim.alertRules()
          .createOrUpdate(mrgName, workspaceName, UUID.randomUUID().toString(), alertRule);
    } catch (ManagementException e) {
      if (e.getValue().getCode().equals("BadRequest")
          && e.getValue()
              .getMessage()
              .contains("already installed")) { // TODO better error matching
        logger.info("Rule {} already installed, skipping", templateName);
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  private ScheduledAlertRule buildScheduledAlertRuleFromTemplate(
      String mrgName, String workspaceName, String ruleTemplateId) {
    var securityInsightsManager = armManagers.securityInsightsManager();
    var template =
        (AlertRuleTemplateImpl)
            securityInsightsManager
                .alertRuleTemplates()
                .get(mrgName, workspaceName, ruleTemplateId);
    var scheduledtemplate = (ScheduledAlertRuleTemplate) template.innerModel();

    return new ScheduledAlertRule()
        .withQuery(scheduledtemplate.query())
        .withAlertRuleTemplateName(scheduledtemplate.name())
        .withDisplayName(scheduledtemplate.displayName())
        .withSuppressionEnabled(false)
        .withSuppressionDuration(Duration.parse("PT1H"))
        .withQueryFrequency(scheduledtemplate.queryFrequency())
        .withQueryPeriod(scheduledtemplate.queryPeriod())
        .withTriggerOperator(scheduledtemplate.triggerOperator())
        .withTriggerThreshold(scheduledtemplate.triggerThreshold())
        .withTactics(scheduledtemplate.tactics())
        .withTechniques(scheduledtemplate.techniques())
        .withAlertDetailsOverride(scheduledtemplate.alertDetailsOverride())
        .withCustomDetails(scheduledtemplate.customDetails())
        .withEntityMappings(scheduledtemplate.entityMappings())
        .withEventGroupingSettings(scheduledtemplate.eventGroupingSettings())
        .withTemplateVersion(scheduledtemplate.version())
        .withEnabled(true)
        .withSeverity(scheduledtemplate.severity());
  }

  private NrtAlertRule buildNrtAlertRuleFromTemplate(
      String mrgName, String workspaceName, String ruleTemplateId) {
    var securityInsightsManager = armManagers.securityInsightsManager();
    var template =
        (AlertRuleTemplateImpl)
            securityInsightsManager
                .alertRuleTemplates()
                .get(mrgName, workspaceName, ruleTemplateId);
    var nrtTemplate = (NrtAlertRuleTemplate) template.innerModel();
    return new NrtAlertRule()
        .withQuery(nrtTemplate.query())
        .withAlertRuleTemplateName(nrtTemplate.name())
        .withDisplayName(nrtTemplate.displayName())
        .withSuppressionEnabled(false)
        .withSuppressionDuration(Duration.parse("PT1H"))
        .withTactics(nrtTemplate.tactics())
        .withTechniques(nrtTemplate.techniques())
        .withAlertDetailsOverride(nrtTemplate.alertDetailsOverride())
        .withCustomDetails(nrtTemplate.customDetails())
        .withEntityMappings(nrtTemplate.entityMappings())
        .withEventGroupingSettings(nrtTemplate.eventGroupingSettings())
        .withTemplateVersion(nrtTemplate.version())
        .withEnabled(true)
        .withSeverity(nrtTemplate.severity());
  }
}
