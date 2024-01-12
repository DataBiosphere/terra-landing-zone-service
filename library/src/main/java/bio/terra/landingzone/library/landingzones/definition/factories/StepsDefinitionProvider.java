package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public interface StepsDefinitionProvider {
  /**
   * Represents code name for a landing zone. The value should be defined in the {@link
   * bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionFactoryType} and
   * reused here. The api which returns a list of all available landing zone will use this value.
   *
   * @return String representation of the code name of a landing zone.
   */
  String landingZoneDefinition();

  /**
   * Represents user-friendly name and description of a landing zone.
   *
   * @return Name and description of a landing zone.
   */
  DefinitionHeader header();

  /**
   * Represents a list of all available versions for the current landing zone definition. Currently,
   * all definitions support only one version called "v1".
   *
   * @return List of supported versions.
   */
  List<DefinitionVersion> availableVersions();

  /**
   * Returns list of required steps for LZ flight together with corresponding retry rule. Each step
   * creates one specific landing zone resource. Order of steps matters. If a resource1 has
   * dependency on another resource2 then resource1 should be deployed first and save its id or
   * another required information for resource2 (using flight working map). So, step for resource1
   * should go first in the list definition.
   *
   * @param ArmManagers armManagers
   * @param ParametersResolverProvider parametersResolverProvider
   * @param ResourceNameProvider resourceNameProvider
   * @param LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration
   * @return List of pairs of steps and step's retry rule
   */
  List<Pair<Step, RetryRule>> get(
      ArmManagers armManagers,
      ParametersResolverProvider parametersResolverProvider,
      ResourceNameProvider resourceNameProvider,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration);
}
