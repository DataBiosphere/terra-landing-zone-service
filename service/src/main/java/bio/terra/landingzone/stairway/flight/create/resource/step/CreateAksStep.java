package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.containerservice.models.AgentPoolMode;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import com.azure.resourcemanager.containerservice.models.ManagedClusterAddonProfile;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAksStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateAksStep.class);
  public static final String AKS_ID = "AKS_ID";

  public CreateAksStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);

    var aksName = resourceNameGenerator.nextName(ResourceNameGenerator.MAX_AKS_CLUSTER_NAME_LENGTH);
    var logAnalyticsWorkspaceId =
        getParameterOrThrow(
            context.getWorkingMap(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            String.class);
    var vNetId = getParameterOrThrow(context.getWorkingMap(), CreateVnetStep.VNET_ID, String.class);

    try {
      final Map<String, ManagedClusterAddonProfile> addonProfileMap = new HashMap<>();
      addonProfileMap.put(
          "omsagent",
          new ManagedClusterAddonProfile()
              .withEnabled(true)
              .withConfig(Map.of("logAnalyticsWorkspaceResourceID", logAnalyticsWorkspaceId)));

      var aksPartial =
          armManagers
              .azureResourceManager()
              .kubernetesClusters()
              .define(aksName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup)
              .withDefaultVersion()
              .withSystemAssignedManagedServiceIdentity()
              .defineAgentPool(
                  resourceNameGenerator.nextName(
                      ResourceNameGenerator.MAX_AKS_AGENT_POOL_NAME_LENGTH))
              .withVirtualMachineSize(
                  ContainerServiceVMSizeTypes.fromString(
                      parametersResolver.getValue(
                          CromwellBaseResourcesFactory.ParametersNames.AKS_MACHINE_TYPE.name())))
              .withAgentPoolVirtualMachineCount(
                  Integer.parseInt(
                      parametersResolver.getValue(
                          CromwellBaseResourcesFactory.ParametersNames.AKS_NODE_COUNT.name())))
              .withAgentPoolMode(AgentPoolMode.SYSTEM)
              .withVirtualNetwork(vNetId, CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name());

      if (Boolean.parseBoolean(
          parametersResolver.getValue(
              CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_ENABLED.name()))) {
        int min =
            Integer.parseInt(
                parametersResolver.getValue(
                    CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MIN.name()));
        int max =
            Integer.parseInt(
                parametersResolver.getValue(
                    CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MAX.name()));
        aksPartial = aksPartial.withAutoScaling(min, max);
      }

      var aks =
          aksPartial
              .attach()
              .withDnsPrefix(
                  resourceNameGenerator.nextName(
                      ResourceNameGenerator.MAX_AKS_DNS_PREFIX_NAME_LENGTH))
              .withAddOnProfiles(addonProfileMap)
              .withTags(
                  Map.of(
                      LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                      landingZoneId.toString(),
                      LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
                      ResourcePurpose.SHARED_RESOURCE.toString()))
              .create();
      context.getWorkingMap().put(AKS_ID, aks.id());
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(RESOURCE_ALREADY_EXISTS, "AKS", aksName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "aks", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) {
    var aksId = context.getWorkingMap().get(AKS_ID, String.class);
    try {
      armManagers.azureResourceManager().kubernetesClusters().deleteById(aksId);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }
}
