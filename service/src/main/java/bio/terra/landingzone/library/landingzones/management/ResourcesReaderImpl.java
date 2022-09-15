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
  private static final ClientLogger logger = new ClientLogger(ResourcesReaderImpl.class);

  private final AzureResourceManager azureResourceManager;
  private final ResourceGroup resourceGroup;

  public ResourcesReaderImpl(
      AzureResourceManager azureResourceManager, ResourceGroup resourceGroup) {
    this.azureResourceManager = azureResourceManager;
    this.resourceGroup = resourceGroup;
  }

  @Override
  public List<DeployedResource> listSharedResources(String landingZoneId) {
    return listResourceByTag(
        landingZoneId,
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        ResourcePurpose.SHARED_RESOURCE.toString());
  }

  @Override
  public List<DeployedResource> listResourcesByPurpose(
      String landingZoneId, ResourcePurpose purpose) {
    return listResourceByTag(
        landingZoneId,
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        purpose.toString());
  }

  @Override
  public List<DeployedResource> listResourcesWithPurpose(String landingZoneId) {
    return listResourceByTag(
        landingZoneId,
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        null);
  }

  @Override
  public List<DeployedVNet> listVNetBySubnetPurpose(
      String landingZoneId, SubnetResourcePurpose purpose) {
    return listResourceByTag(landingZoneId, resourceGroup.name(), purpose.toString(), null).stream()
        .map(this::toDeployedVNet)
        .collect(Collectors.toList());
  }

  @Override
  public List<DeployedSubnet> listSubnetsBySubnetPurpose(
      String landingZoneId, SubnetResourcePurpose purpose) {
    return listResourceByTag(landingZoneId, resourceGroup.name(), purpose.toString(), null).stream()
        .map(r -> toDeployedSubnet(r, purpose))
        .toList();
  }

  private List<DeployedResource> listResourceByTag(
      String landingZoneId, String resourceGroup, String key, String value) {
    logger.info("Listing resources by tag. group:{} key:{} value:{} ", resourceGroup, key, value);
    return this.azureResourceManager
        .genericResources()
        .listByTag(resourceGroup, LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId)
        .stream()
        .filter(
            r -> r.tags().containsKey(key) && (r.tags().get(key).equals(value) || value == null))
        .map(this::toLandingZoneDeployedResource)
        .collect(Collectors.toList());
  }

  private DeployedResource toLandingZoneDeployedResource(GenericResource r) {
    logger.info(
        "To landing zone deployed resource: {} type: {} tags: {}", r.id(), r.type(), r.tags());
    return new DeployedResource(r.id(), r.type(), r.tags(), r.region().name());
  }

  private DeployedVNet toDeployedVNet(DeployedResource resource) {
    Network vNet = azureResourceManager.networks().getById(resource.resourceId());

    if (vNet == null) {
      throw logger.logExceptionAsError(
          new RuntimeException(
              "The resource provided is not VNet or the resource is no longer available"));
    }

    return toDeployedVNet(vNet);
  }

  private DeployedSubnet toDeployedSubnet(
      DeployedResource resource, SubnetResourcePurpose purpose) {
    Network vNet = azureResourceManager.networks().getById(resource.resourceId());

    if (vNet == null) {
      throw logger.logExceptionAsError(
          new RuntimeException(
              "The resource provided is not VNet or the resource is no longer available"));
    }

    var subnetName = vNet.tags().get(purpose.toString());
    var subnet = vNet.subnets().get(subnetName);

    return new DeployedSubnet(subnet.id(), subnetName, vNet.id(), vNet.regionName());
  }

  private DeployedVNet toDeployedVNet(Network network) {
    HashMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap = new HashMap<>();

    SubnetResourcePurpose.values()
        .forEach(
            p -> {
              var subnetName = network.tags().get(p.toString());
              if (subnetName != null) {
                var subnet = network.subnets().get(subnetName);
                subnetHashMap.put(
                    p,
                    new DeployedSubnet(
                        subnet.id(), subnet.name(), network.id(), network.regionName()));
              }
            });

    return new DeployedVNet(network.id(), subnetHashMap, network.regionName());
  }
}
