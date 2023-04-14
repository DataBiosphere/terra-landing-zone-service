package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.stairway.flight.create.CreateLogAnalyticsWorkspaceStep;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class ProtectedDataStepsDefinitionProvider implements StepsDefinitionProvider {
  @Override
  public List<Pair<Step, RetryRule>> get(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration) {
    return List.of(
        Pair.of(
            new CreateLogAnalyticsWorkspaceStep(landingZoneAzureConfiguration),
            RetryRules.cloud()));
  }
}
