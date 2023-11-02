package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.LandingZoneDefinable;
import java.util.List;

/**
 * Operations of a Landing Zone Definition Factory: creation of definitions by version, listing of
 * the available versions and the header
 */
public interface LandingZoneDefinitionFactory {

  DefinitionHeader header();

  List<DefinitionVersion> availableVersions();

  LandingZoneDefinable create(DefinitionVersion version);
}
