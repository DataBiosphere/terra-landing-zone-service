package bio.terra.landingzone.library.landingzones.deployment;

import java.util.Map;

/**
 * Record of a resource deployed in a landing zone.
 *
 * @param resourceId resource id.
 * @param resourceType resource type.
 * @param tags tags map.
 * @param region region name.
 */
public record DeployedResource(
    String resourceId, String resourceType, Map<String, String> tags, String region, String name) {}
