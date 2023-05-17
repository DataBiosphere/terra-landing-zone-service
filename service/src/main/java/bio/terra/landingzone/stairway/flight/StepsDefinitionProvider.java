package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import com.azure.resourcemanager.AzureResourceManager;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public interface StepsDefinitionProvider {
  /**
   * Returns list of required together with corresponding retry rule. Each step creates one specific
   * landing zone resource. Order of steps matters. If a resource1 has dependency on another
   * resource2 then resource1 should be deployed first and save its id or another required
   * information for resource2 (using flight working map). So, step for resource1 should go first in
   * the list definition.
   *
   * @param lzArmManagers Collection of Azure resource managers that are able to connect to the user
   *     MRG
   * @param adminSubResourceManager Azure resource manager that is able to connect to a Terra
   *     administrative subscription
   * @param parametersResolver parametersResolver Landing zone parameter resolver
   * @param resourceNameGenerator resourceNameGenerator Landing zone resource name generator
   * @param landingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration Protected
   *     data landing specific configuration
   * @return List of pairs of steps and step's retry rule
   */
  List<Pair<Step, RetryRule>> get(
      ArmManagers lzArmManagers,
      AzureResourceManager adminSubResourceManager,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration);
}
