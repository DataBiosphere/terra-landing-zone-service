package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.DeployedVNet;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import java.util.List;

/**
 * Provides different search operations for a resources in specific landing zone. All resources in
 * landing zone have different set of tags assigned. Each tag has its own purpose. All the search
 * operation are based on tags.
 *
 * <p>Tag examples: WLZ-PURPOSE - defines purpose for a specific resource WLZ-ID - defines landing
 * zone identifier
 */
public interface ResourcesReader {
  /**
   * Lists shared resources belonging to a specific landing zone.
   *
   * @param landingZoneId Identifier of a landing zone
   * @return Resource(s) which correspond(s) search criteria
   */
  List<DeployedResource> listSharedResources(String landingZoneId);

  List<DeployedResource> listResourcesByPurpose(String landingZoneId, ResourcePurpose purpose);

  List<DeployedResource> listResources();

  List<DeployedVNet> listVNetWithSubnetPurpose(SubnetResourcePurpose purpose);

  List<DeployedSubnet> listSubnetsWithSubnetPurpose(SubnetResourcePurpose purpose);
}
