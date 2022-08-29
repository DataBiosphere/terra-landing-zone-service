package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.List;
import java.util.Map;

/**
 * Record of a Resources list deployed in a landing zone.
 *
 * @param generalResources map of general resources and their purposes.
 * @param subnetResources map of subnet resources and their purpose.
 */
public record LandingZoneResourcesByPurpose(
    Map<String, List<LandingZoneResource>> generalResources,
    Map<String, List<LandingZoneSubnetResource>> subnetResources) {}
;
