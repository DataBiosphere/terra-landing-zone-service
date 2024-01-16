package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.stairway.FlightContext;
import com.azure.resourcemanager.securityinsights.models.AutomationRuleRunPlaybookAction;
import com.azure.resourcemanager.securityinsights.models.AutomationRuleTriggeringLogic;
import com.azure.resourcemanager.securityinsights.models.PlaybookActionProperties;
import com.azure.resourcemanager.securityinsights.models.TriggersOn;
import com.azure.resourcemanager.securityinsights.models.TriggersWhen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateSentinelRunPlaybookAutomationRule extends BaseResourceCreateStep {
  private static final Logger logger =
      LoggerFactory.getLogger(CreateSentinelRunPlaybookAutomationRule.class);

  private final LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration;

  public CreateSentinelRunPlaybookAutomationRule(
      ResourceNameProvider resourceNameProvider,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    super(resourceNameProvider);
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

    var trigger =
        new AutomationRuleTriggeringLogic()
            .withTriggersWhen(TriggersWhen.CREATED)
            .withTriggersOn(TriggersOn.INCIDENTS)
            .withIsEnabled(true);
    var runPlaybookAction =
        new AutomationRuleRunPlaybookAction()
            .withOrder(1)
            .withActionConfiguration(
                new PlaybookActionProperties()
                    .withLogicAppResourceId(
                        landingZoneProtectedDataConfiguration.getLogicAppResourceId())
                    .withTenantId(
                        UUID.fromString(landingZoneProtectedDataConfiguration.getTenantId())));
    var automationRule =
        armManagers
            .securityInsightsManager()
            .automationRules()
            .define("runSendSlackNotificationPlaybook")
            .withExistingWorkspace(getMRGName(context), logAnalyticsWorkspaceResourceName.get())
            .withDisplayName("Run Slack Notification Playbook")
            .withOrder(1)
            .withTriggeringLogic(trigger)
            .withActions(List.of(runPlaybookAction))
            .create();
    logger.info(RESOURCE_CREATED, getResourceType(), automationRule.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId, ArmManagers armManagers) {
    // nothing to delete here since an automation rule will be deleted together with sentinel
  }

  @Override
  protected String getResourceType() {
    return "SentinelAutomationRule";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.empty();
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of();
  }
}
