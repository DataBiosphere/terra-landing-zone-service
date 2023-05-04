package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelRunPlaybookAutomationRule;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelStep;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class ProtectedDataStepsDefinitionProvider extends CromwellStepsDefinitionProvider {
  @Override
  public List<Pair<Step, RetryRule>> get(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    // inherit all cromwell steps and define specific below
    var protectedDataSteps =
        new ArrayList<>(
            super.get(
                armManagers,
                parametersResolver,
                resourceNameGenerator,
                landingZoneProtectedDataConfiguration));

    protectedDataSteps.add(
        Pair.of(
            new CreateSentinelStep(armManagers, parametersResolver, resourceNameGenerator),
            RetryRules.cloud()));

    protectedDataSteps.add(
        Pair.of(
            new CreateSentinelRunPlaybookAutomationRule(
                armManagers,
                parametersResolver,
                resourceNameGenerator,
                landingZoneProtectedDataConfiguration),
            RetryRules.cloud()));

    return protectedDataSteps;
  }
}
