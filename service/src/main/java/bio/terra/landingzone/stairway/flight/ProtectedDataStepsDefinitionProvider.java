package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class ProtectedDataStepsDefinitionProvider extends CromwellStepsDefinitionProvider {
  @Override
  public List<Pair<Step, RetryRule>> get(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    var cromwellBaseSteps = super.get(landingZoneAzureConfiguration, resourceNameGenerator);

    // add steps specific to protected data

    return cromwellBaseSteps;
  }
}
