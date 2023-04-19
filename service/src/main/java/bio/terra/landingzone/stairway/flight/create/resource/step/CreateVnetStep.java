package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import bio.terra.stairway.exception.RetryException;
import com.azure.core.management.exception.ManagementException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateVnetStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateVnetStep.class);
  public static final String VNET_ID = "VNET_ID";

  public CreateVnetStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult doStep(FlightContext context) throws InterruptedException, RetryException {
    super.doStep(context);
    // TODO: check if we can arrange all these dependencies in a different way
    // Most like we need the same setup for different steps. At least we need armManagers.
    String vNetName = resourceNameGenerator.nextName(ResourceNameGenerator.MAX_VNET_NAME_LENGTH);
    try {
      var vNet =
          armManagers
              .azureResourceManager()
              .networks()
              .define(vNetName)
              .withRegion(resourceGroup.region())
              .withExistingResourceGroup(resourceGroup)
              .withAddressSpace(
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.ParametersNames.VNET_ADDRESS_SPACE.name()))
              .withSubnet(
                  CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name(),
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name()))
              .withSubnet(
                  CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name(),
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name()))
              .withSubnet(
                  CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name(),
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name()))
              .withSubnet(
                  CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name(),
                  parametersResolver.getValue(
                      CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name()))
              .withTags(
                  Map.of(
                      CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name(),
                      SubnetResourcePurpose.AKS_NODE_POOL_SUBNET.toString(),
                      CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name(),
                      SubnetResourcePurpose.WORKSPACE_BATCH_SUBNET.toString(),
                      CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name(),
                      SubnetResourcePurpose.POSTGRESQL_SUBNET.toString(),
                      CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name(),
                      SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString()))
              .create();
      context.getWorkingMap().put(VNET_ID, vNet.id());
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "conflict")) {
        logger.info(RESOURCE_ALREADY_EXISTS, "Virtual network", vNetName, resourceGroup.name());
        return StepResult.getStepResultSuccess();
      }
      logger.error(FAILED_TO_CREATE_RESOURCE, "virtual network", landingZoneId.toString());
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    var vNetId = context.getWorkingMap().get(VNET_ID, String.class);
    try {
      armManagers.azureResourceManager().networks().deleteById(vNetId);
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        return StepResult.getStepResultSuccess();
      }
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }
}
