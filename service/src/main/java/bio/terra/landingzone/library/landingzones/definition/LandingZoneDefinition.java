package bio.terra.landingzone.library.landingzones.definition;

/**
 * Base class for a Landing Zone Definition that contains the ARM clients to define the resource to
 * deploy
 */
public abstract class LandingZoneDefinition implements LandingZoneDefinable {

  protected final ArmManagers armManagers;

  protected LandingZoneDefinition(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }
}
