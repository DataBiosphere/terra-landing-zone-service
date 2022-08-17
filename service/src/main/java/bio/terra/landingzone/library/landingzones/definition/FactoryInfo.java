package bio.terra.landingzone.library.landingzones.definition;

import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactory;
import java.util.List;

/**
 * Contains the class instance and the list of available versions of a {@link
 * LandingZoneDefinitionFactory}
 */
public record FactoryInfo(
    Class<? extends LandingZoneDefinitionFactory> factoryClass, List<DefinitionVersion> versions) {}
