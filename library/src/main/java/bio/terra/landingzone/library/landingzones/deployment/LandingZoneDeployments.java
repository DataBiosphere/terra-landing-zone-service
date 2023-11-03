package bio.terra.landingzone.library.landingzones.deployment;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment.DefinitionStages.WithLandingZoneResource;

/** Factory entry point for the creation deployments with a fluent interface. */
public interface LandingZoneDeployments {
  WithLandingZoneResource define(String landingZoneId);
}
