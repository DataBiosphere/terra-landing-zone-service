package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateVnetStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateVnetStep.class);
  public static final String VNET_ID = "VNET_ID";
  public static final String VNET_RESOURCE_KEY = "VNET";

  public CreateVnetStep(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    super(landingZoneAzureConfiguration, resourceNameGenerator);
  }

  @Override
  public StepResult undoStep(FlightContext context) throws InterruptedException {
    var vNetId = context.getWorkingMap().get(VNET_ID, String.class);
    try {
      if (vNetId != null) {
        // armManagers.azureResourceManager().networks().deleteById(vNetId);
        armManagers.azureResourceManager().genericResources().deleteById(vNetId);
      }
    } catch (ManagementException e) {
      if (StringUtils.equalsIgnoreCase(e.getValue().getCode(), "ResourceNotFound")) {
        logger.error("Virtual network doesn't exist or has been already deleted. Id={}", vNetId);
        return StepResult.getStepResultSuccess();
      }
      logger.error("Failed attempt to delete virtual network. Id={}", vNetId);
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_RETRY, e);
    }
    return StepResult.getStepResultSuccess();
  }

  @Override
  public void createResource(FlightContext context, ArmManagers armManagers) {
    String vNetName = resourceNameGenerator.nextName(ResourceNameGenerator.MAX_VNET_NAME_LENGTH);
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
                parametersResolver.getValue(CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name()))
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
                    LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                    landingZoneId.toString(),
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
    context
        .getWorkingMap()
        .put(
            VNET_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(vNet.id())
                .resourceType(vNet.type())
                .tags(vNet.tags())
                .region(vNet.regionName())
                .resourceName(vNet.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), vNet.id(), resourceGroup.name());
  }

  @Override
  public String getResourceType() {
    return "VirtualNetwork";
  }
}
