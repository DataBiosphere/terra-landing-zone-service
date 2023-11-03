package bio.terra.landingzone.library.landingzones.definition.factories;

/** High-level API for listing and creating Landing Zones Definition factories. */
public interface LandingZoneDefinitionProvider {

  <T extends LandingZoneDefinitionFactory> LandingZoneDefinitionFactory createDefinitionFactory(
      Class<T> factory);
}
