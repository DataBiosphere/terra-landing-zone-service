package bio.terra.landingzone.library.landingzones.definition;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment;

/** Enables the definition of a Landing Zone */
public interface LandingZoneDefinable {
  LandingZoneDeployment.DefinitionStages.Deployable definition(DefinitionContext definitionContext);
}
