package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.DeployedVNet;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ResourcesReaderImpl implements ResourcesReader {
  private final AzureResourceManager azureResourceManager;
  private final ResourceGroup resourceGroup;

  private final ClientLogger logger = new ClientLogger(ResourcesReaderImpl.class);

  public ResourcesReaderImpl(
      AzureResourceManager azureResourceManager, ResourceGroup resourceGroup) {
    this.azureResourceManager = azureResourceManager;
    this.resourceGroup = resourceGroup;
  }

  @Override
  public List<DeployedResource> listSharedResources() {
    return listResourceByTag(
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        ResourcePurpose.SHARED_RESOURCE.toString());
  }

  private List<DeployedResource> listResourceByTag(String resourceGroup, String key, String value) {
    return this.azureResourceManager
        .genericResources()
        .listByTag(resourceGroup, key, value)
        .stream()
        .map(this::toLandingZoneDeployedResource)
        .collect(Collectors.toList());
  }

  private DeployedResource toLandingZoneDeployedResource(GenericResource r) {
    return new DeployedResource(r.id(), r.type(), r.tags(), r.region().name());
  }

  @Override
  public List<DeployedResource> listResourcesByPurpose(ResourcePurpose purpose) {
    return listResourceByTag(
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        purpose.toString());
  }

  @Override
  public List<DeployedVNet> listVNetWithSubnetPurpose(SubnetResourcePurpose purpose) {
    return listResourceByTag(resourceGroup.name(), purpose.toString(), null).stream()
        .map(this::toDeployedVNet)
        .collect(Collectors.toList());
  }

  private DeployedVNet toDeployedVNet(DeployedResource resource) {
    Network vNet = azureResourceManager.networks().getById(resource.resourceId());

    if (vNet == null) {
      logger.logExceptionAsError(
          new RuntimeException(
              "The resource provided is not VNet or the resource is no longer available"));
    }

    return toDeployedVNet(vNet);
  }

  private DeployedVNet toDeployedVNet(Network network) {
    HashMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap = new HashMap<>();

    SubnetResourcePurpose.values()
        .forEach(
            p -> {
              var subnetName = network.tags().get(p.toString());
              if (subnetName != null) {
                var subnet = network.subnets().get(subnetName);
                subnetHashMap.put(p, new DeployedSubnet(subnet.id(), subnet.name()));
              }
            });

    return new DeployedVNet(network.id(), subnetHashMap, network.regionName());
  }
}
