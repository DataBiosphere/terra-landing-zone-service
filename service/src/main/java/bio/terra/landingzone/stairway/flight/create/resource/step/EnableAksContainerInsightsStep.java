package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.common.utils.HttpResponseUtils;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.stairway.flight.utils.FlightUtils;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.ManagedClusterAddonProfile;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This step turns on Container insights (monitoring) for AKS by applying the specific add-on
 * profile, specifying "useAADAuth=true". Container insights must be enabled after AKS creation, and
 * after the AKS monitoring configuration is created (for instance data collection rule and rule
 * association in case of cost optimization settings or ContainerLogV2). Attempting to enable
 * container insights at the time of AKS creation resulted in broken AKS monitoring.
 */
public class EnableAksContainerInsightsStep implements Step {
  private static final Logger logger =
      LoggerFactory.getLogger(EnableAksContainerInsightsStep.class);

  private final ArmManagers armManagers;

  public EnableAksContainerInsightsStep(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    FlightUtils.validateRequiredEntries(
        context.getWorkingMap(), CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID);
    var logAnalyticsWorkspaceId =
        FlightUtils.getRequired(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    FlightUtils.validateRequiredEntries(context.getWorkingMap(), CreateAksStep.AKS_ID);
    var aksId =
        FlightUtils.getRequired(context.getWorkingMap(), CreateAksStep.AKS_ID, String.class);

    final Map<String, ManagedClusterAddonProfile> addonProfileMap = new HashMap<>();
    addonProfileMap.put(
        "omsagent",
        new ManagedClusterAddonProfile()
            .withEnabled(true)
            .withConfig(
                Map.of(
                    "logAnalyticsWorkspaceResourceID",
                    logAnalyticsWorkspaceId,
                    "useAADAuth",
                    "true")));
    try {
      var aks = armManagers.azureResourceManager().kubernetesClusters().getById(aksId);
      KubernetesCluster.Update aksToUpdate = aks.update();
      aksToUpdate.withAddOnProfiles(addonProfileMap).apply();
      logger.info("Container insights for AKS with id='{}' has been enabled.", aksId);
    } catch (ManagementException e) {
      if (HttpResponseUtils.isRetryable(e.getResponse().getStatusCode())) {
        logger.error("Failed attempt to enable container insights for AKS with id='{}'", aksId);
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
      } else {
        logger.error("Failed to enable container insights for AKS with id='{}'", aksId);
        return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
      }
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    // no need to disable container insights
    return StepResult.getStepResultSuccess();
  }
}
