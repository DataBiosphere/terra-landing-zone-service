package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.landingzone.stairway.flight.utils.AlertRulesHelper;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.securityinsights.models.MLBehaviorAnalyticsAlertRule;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSentinelAlertRulesStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateSentinelAlertRulesStep.class);
  private AlertRulesHelper alertRuleAdapter;
  private LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration;

  public CreateSentinelAlertRulesStep(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator,
      AlertRulesHelper alertRuleAdapter,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    super(armManagers, parametersResolver, resourceNameGenerator);
    this.alertRuleAdapter = alertRuleAdapter;
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
                  alertRuleAdapter.buildScheduledAlertRuleFromTemplate(
                      mrgName, workspaceName, ruleTemplateId);
              alertRuleAdapter.createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
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
              alertRuleAdapter.createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
            });
  }

  private void createNrtAlertRules(String mrgName, String workspaceName) {
    landingZoneProtectedDataConfiguration
        .getSentinelNrtRuleTemplateIds()
        .forEach(
            ruleTemplateId -> {
              var rule =
                  alertRuleAdapter.buildNrtAlertRuleFromTemplate(
                      mrgName, workspaceName, ruleTemplateId);
              alertRuleAdapter.createAlertRule(rule, ruleTemplateId, mrgName, workspaceName);
            });
  }
}
