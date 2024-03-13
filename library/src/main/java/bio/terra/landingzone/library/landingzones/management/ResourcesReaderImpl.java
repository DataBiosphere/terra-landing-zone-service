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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides search operations for a resources in specific landing zone. All resources in landing
 * zone have different set of tags assigned. Each tag has its own purpose. All the search operation
 * are based on tags.
 *
 * <p>Tag examples:
 *
 * <p>WLZ-PURPOSE - defines purpose for a specific resource;
 *
 * <p>WLZ-ID - defines landing zone identifier
 */
public class ResourcesReaderImpl implements ResourcesReader {
  private static final ClientLogger logger = new ClientLogger(ResourcesReaderImpl.class);

  private final AzureResourceManager azureResourceManager;
  private final ResourceGroup resourceGroup;

  public ResourcesReaderImpl(
      AzureResourceManager azureResourceManager, ResourceGroup resourceGroup) {
    this.azureResourceManager = azureResourceManager;
    this.resourceGroup = resourceGroup;
  }

  /**
   * Lists shared resources in a specific landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @return the list of resources
   */
  @Override
  public List<DeployedResource> listSharedResources(String landingZoneId) {
    return listResourcesByTag(
        landingZoneId,
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        ResourcePurpose.SHARED_RESOURCE.toString());
  }

  /**
   * Lists resources with specific purpose in a landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @param purpose purpose's value
   * @return the list of resources
   */
  @Override
  public List<DeployedResource> listResourcesByPurpose(
      String landingZoneId, ResourcePurpose purpose) {
    return listResourcesByTag(
        landingZoneId,
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        purpose.toString());
  }

  /**
   * Lists all resources with purpose in a specific landing zone. Only resources with Landing Zone
   * Purpose will be returned.
   *
   * @param landingZoneId the identifier of the landing zone
   * @return the list of resources
   */
  @Override
  public List<DeployedResource> listResourcesWithPurpose(String landingZoneId) {
    return listResourcesByTag(
        landingZoneId,
        resourceGroup.name(),
        LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
        null);
  }

  /**
   * Lists all virtual networks with specific subnet resource purpose in a landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @param purpose purpose's value
   * @return the list of virtual networks
   */
  @Override
  public List<DeployedVNet> listVNetBySubnetPurpose(
      String landingZoneId, SubnetResourcePurpose purpose) {
    return listResourcesByTag(landingZoneId, resourceGroup.name(), purpose.toString(), null)
        .stream()
        .map(this::toDeployedVNet)
        .collect(Collectors.toList());
  }

  @Override
  public List<DeployedResource> listAllResources(String landingZoneId) {
    return landingZoneResources(landingZoneId, resourceGroup.name())
        .map(this::toLandingZoneDeployedResource)
        .toList();
  }

  /**
   * Lists all subnets with specific subnet purpose in a landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @param purpose purpose's value
   * @return the list of subnets
   */
  @Override
  public List<DeployedSubnet> listSubnetsBySubnetPurpose(
      String landingZoneId, SubnetResourcePurpose purpose) {
    return listResourcesByTag(landingZoneId, resourceGroup.name(), purpose.toString(), null)
        .stream()
        .map(r -> toDeployedSubnet(r, purpose))
        .toList();
  }

  private List<DeployedResource> listResourcesByTag(
      String landingZoneId, String resourceGroup, String key, String value) {
    logger.verbose(
        "Listing resources by tag. lzid:{} group:{} key:{} value:{} ",
        landingZoneId,
        resourceGroup,
        key,
        value);
    return landingZoneResources(landingZoneId, resourceGroup)
        .filter(
            r -> r.tags().containsKey(key) && (r.tags().get(key).equals(value) || value == null))
        .map(this::toLandingZoneDeployedResource)
        .collect(Collectors.toList());
  }

  private Stream<GenericResource> landingZoneResources(String landingZoneId, String resourceGroup) {
    return this.azureResourceManager
        .genericResources()
        .listByTag(resourceGroup, LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId)
        .stream();
  }

  private DeployedResource toLandingZoneDeployedResource(GenericResource r) {
    logger.verbose(
        "To landing zone deployed resource: {} type: {} tags: {}", r.id(), r.type(), r.tags());
    return new DeployedResource(r.id(), r.type(), r.tags(), r.region().name(), r.name());
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
    EnumMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap =
        new EnumMap<>(SubnetResourcePurpose.class);

    Arrays.stream(SubnetResourcePurpose.values())
        .toList()
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
