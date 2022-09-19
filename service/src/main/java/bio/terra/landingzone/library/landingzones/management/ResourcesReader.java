package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.DeployedVNet;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import java.util.List;

public interface ResourcesReader {
  List<DeployedResource> listSharedResources(String landingZoneId);

  List<DeployedResource> listResourcesByPurpose(String landingZoneId, ResourcePurpose purpose);

  List<DeployedResource> listResourcesWithPurpose(String landingZoneId);

  List<DeployedVNet> listVNetBySubnetPurpose(String landingZoneId, SubnetResourcePurpose purpose);

  List<DeployedSubnet> listSubnetsBySubnetPurpose(
      String landingZoneId, SubnetResourcePurpose purpose);
}
