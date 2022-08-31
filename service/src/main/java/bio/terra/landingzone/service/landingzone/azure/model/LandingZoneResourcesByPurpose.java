package bio.terra.landingzone.service.landingzone.azure.model;

import bio.terra.landingzone.library.landingzones.deployment.LandingZonePurpose;
import java.util.List;
import java.util.Map;

/**
 * Record of a Resources list deployed in a landing zone.
 *
 * @param deployedResources map of general resources and their purposes.
 */
public record LandingZoneResourcesByPurpose(
    Map<LandingZonePurpose, List<LandingZoneResource>> deployedResources) {}
