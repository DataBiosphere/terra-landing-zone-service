package bio.terra.landingzone.stairway.flight.utils;

import com.azure.resourcemanager.securityinsights.SecurityInsightsManager;
import com.azure.resourcemanager.securityinsights.fluent.models.AlertRuleInner;
import com.azure.resourcemanager.securityinsights.implementation.AlertRuleTemplateImpl;
import com.azure.resourcemanager.securityinsights.models.NrtAlertRule;
import com.azure.resourcemanager.securityinsights.models.NrtAlertRuleTemplate;
import com.azure.resourcemanager.securityinsights.models.ScheduledAlertRule;
import com.azure.resourcemanager.securityinsights.models.ScheduledAlertRuleTemplate;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertRulesHelper {
  private final SecurityInsightsManager securityInsightsManager;
  private static final Logger logger = LoggerFactory.getLogger(AlertRulesHelper.class);

  public AlertRulesHelper(SecurityInsightsManager securityInsightsManager) {
    this.securityInsightsManager = securityInsightsManager;
  }

  public AlertRuleInner buildScheduledAlertRuleFromTemplate(
      String mrgName, String workspaceName, String ruleTemplateId) {
    var template =
        (AlertRuleTemplateImpl)
            securityInsightsManager
                .alertRuleTemplates()
                .get(mrgName, workspaceName, ruleTemplateId);
    var scheduledtemplate = (ScheduledAlertRuleTemplate) template.innerModel();

    return new ScheduledAlertRule()
        .withQuery(scheduledtemplate.query())
        .withDescription(scheduledtemplate.description())
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

  public NrtAlertRule buildNrtAlertRuleFromTemplate(
      String mrgName, String workspaceName, String ruleTemplateId) {
    var template =
        (AlertRuleTemplateImpl)
            securityInsightsManager
                .alertRuleTemplates()
                .get(mrgName, workspaceName, ruleTemplateId);
    var nrtTemplate = (NrtAlertRuleTemplate) template.innerModel();
    return new NrtAlertRule()
        .withQuery(nrtTemplate.query())
        .withDescription(nrtTemplate.description())
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

  public void createAlertRule(
      AlertRuleInner alertRule, String ruleId, String mrgName, String workspaceName) {
    logger.info("Creating alert rule w/id = {}", ruleId);
    securityInsightsManager.alertRules().createOrUpdate(mrgName, workspaceName, ruleId, alertRule);
    logger.info("Finished creating alert rule w/id = {}", ruleId);
  }
}
