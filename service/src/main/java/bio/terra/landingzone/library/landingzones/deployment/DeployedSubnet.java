package bio.terra.landingzone.library.landingzones.deployment;

/**
 * Record of a subnet deployed in a landing zone.
 *
 * @param id subnet id.
 * @param name subnet name.
 * @param vNetId VNet id.
 * @param vNetRegion VNet region.
 */
public record DeployedSubnet(String id, String name, String vNetId, String vNetRegion) {}
