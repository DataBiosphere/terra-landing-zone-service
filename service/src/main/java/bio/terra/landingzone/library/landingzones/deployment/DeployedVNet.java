package bio.terra.landingzone.library.landingzones.deployment;

import java.util.Map;

/**
 * Record of a Virtual Network deployed in a landing zone.
 *
 * @param Id virtual network id.
 * @param subnetIdPurposeMap map of subnet id's and their purpose.
 * @param region region name.
 */
public record DeployedVNet(
    String Id, Map<SubnetResourcePurpose, DeployedSubnet> subnetIdPurposeMap, String region) {}
