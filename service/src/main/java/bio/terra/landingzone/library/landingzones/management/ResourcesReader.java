package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.DeployedVNet;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import java.util.List;

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
public interface ResourcesReader {
  /**
   * Lists shared resources in a specific landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @return the list of resources
   */
  List<DeployedResource> listSharedResources(String landingZoneId);

  /**
   * Lists resources with specific purpose in a landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @param purpose purpose's value
   * @return the list of resources
   */
  List<DeployedResource> listResourcesByPurpose(String landingZoneId, ResourcePurpose purpose);

  /**
   * Lists all resources in a specific landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @return the list of resources
   */
  List<DeployedResource> listResources(String landingZoneId);

  /**
   * Lists all virtual networks with specific purpose in a landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @param purpose purpose's value
   * @return the list of virtual networks
   */
  List<DeployedVNet> listVNetWithSubnetPurpose(String landingZoneId, SubnetResourcePurpose purpose);

  /**
   * Lists all subnets with specific purpose in a landing zone.
   *
   * @param landingZoneId the identifier of the landing zone
   * @param purpose purpose's value
   * @return the list of subnets
   */
  List<DeployedSubnet> listSubnetsWithSubnetPurpose(
      String landingZoneId, SubnetResourcePurpose purpose);
}
