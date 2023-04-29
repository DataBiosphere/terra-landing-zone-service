package bio.terra.landingzone.stairway.common.model;

/**
 * Managed resource group where landing zone will be deployed. This information requires by almost
 * all steps. The very first step of the flight saves this data so each consequent step have access
 * to this information.
 *
 * @param name Name of the managed resource group
 * @param region Region of the managed resource group
 */
public record TargetManagedResourceGroup(String name, String region) {}
