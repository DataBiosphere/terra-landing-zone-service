package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.ServiceEndpointType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class CreateVnetStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateVnetStep.class);
  public static final String VNET_ID = "VNET_ID";
  public static final String VNET_RESOURCE_KEY = "VNET";

  public CreateVnetStep(
      ParametersResolver parametersResolver, ResourceNameProvider resourceNameProvider) {
    super(parametersResolver, resourceNameProvider);
  }

  @Override
  public void createResource(FlightContext context, ArmManagers armManagers) {
    String vNetName = resourceNameProvider.getName(getResourceType());
    var nsgId =
        getParameterOrThrow(
            context.getWorkingMap(), CreateNetworkSecurityGroupStep.NSG_ID, String.class);
    Network vNet = createVnetAndSubnets(context, armManagers, vNetName, nsgId);

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
    logger.info(RESOURCE_CREATED, getResourceType(), vNet.id(), getMRGName(context));
  }

  private Network createVnetAndSubnets(
      FlightContext context,
      ArmManagers armManagers,
      String vNetName,
      String networkSecurityGroupId) {
    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    try {
      return armManagers
          .azureResourceManager()
          .networks()
          .define(vNetName)
          .withRegion(getMRGRegionName(context))
          .withExistingResourceGroup(getMRGName(context))
          .withAddressSpace(
              parametersResolver.getValue(
                  CromwellBaseResourcesFactory.ParametersNames.VNET_ADDRESS_SPACE.name()))
          .defineSubnet(CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name())
          .withAddressPrefix(
              parametersResolver.getValue(CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name()))
          .withExistingNetworkSecurityGroup(networkSecurityGroupId)
          .attach()
          .defineSubnet(CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name())
          .withAddressPrefix(
              parametersResolver.getValue(CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name()))
          .withExistingNetworkSecurityGroup(networkSecurityGroupId)
          .withAccessFromService(ServiceEndpointType.MICROSOFT_STORAGE)
          .withAccessFromService(ServiceEndpointType.MICROSOFT_SQL)
          .attach()
          .defineSubnet(CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name())
          .withAddressPrefix(
              parametersResolver.getValue(
                  CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name()))
          .withExistingNetworkSecurityGroup(networkSecurityGroupId)
          .withDelegation("Microsoft.DBforPostgreSQL/flexibleServers")
          .withAccessFromService(ServiceEndpointType.MICROSOFT_STORAGE)
          .attach()
          .defineSubnet(CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name())
          .withAddressPrefix(
              parametersResolver.getValue(
                  CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name()))
          .withExistingNetworkSecurityGroup(networkSecurityGroupId)
          .attach()
          .withTags(
              Map.of(
                  LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
                  landingZoneId.toString(),
                  SubnetResourcePurpose.AKS_NODE_POOL_SUBNET.toString(),
                  CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name(),
                  SubnetResourcePurpose.WORKSPACE_BATCH_SUBNET.toString(),
                  CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name(),
                  SubnetResourcePurpose.POSTGRESQL_SUBNET.toString(),
                  CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name(),
                  SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString(),
                  CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name()))
          .create();
    } catch (ManagementException e) {
      // resource may already exist if this step is being retried
      if (e.getResponse() != null
          && HttpStatus.CONFLICT.value() == e.getResponse().getStatusCode()) {
        return armManagers
            .azureResourceManager()
            .networks()
            .getByResourceGroup(getMRGName(context), vNetName);
      } else {
        throw e;
      }
    }
  }

  @Override
  protected void deleteResource(String resourceId, FlightContext context) {
    var armManagers =
        context.getWorkingMap().get(LandingZoneFlightMapKeys.ARM_MANAGERS_KEY, ArmManagers.class);
    armManagers.azureResourceManager().networks().deleteById(resourceId);
  }

  @Override
  public String getResourceType() {
    return "VirtualNetwork";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(VNET_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_VNET_NAME_LENGTH));
  }
}
