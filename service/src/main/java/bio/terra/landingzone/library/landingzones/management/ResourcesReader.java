package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.DeployedVNet;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import java.util.List;

public interface ResourcesReader {
  List<DeployedResource> listSharedResources(String landingZoneId);

  List<DeployedResource> listResourcesByPurpose(ResourcePurpose purpose);

  List<DeployedResource> listResources();

  List<DeployedVNet> listVNetWithSubnetPurpose(SubnetResourcePurpose purpose);

  List<DeployedSubnet> listSubnetsWithSubnetPurpose(SubnetResourcePurpose purpose);
}
