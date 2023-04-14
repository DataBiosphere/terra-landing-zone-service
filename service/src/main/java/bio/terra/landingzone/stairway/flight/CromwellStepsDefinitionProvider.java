package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class CromwellStepsDefinitionProvider implements StepsDefinitionProvider {
  @Override
  public List<Pair<Step, RetryRule>> get(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration) {
    /*
     * ~ - depends on
     * 1) VNet step
     * 2) Log analytics step
     * 3) Postgres
     * 4) Storage account
     * 5) Batch account
     *
     * 6) Cors rules ~  4)
     * 7) Data collection rules ~ 3)
     * 8) Private endpoint ~ 1), 2)
     * 9) AKS ~ 1)
     * 10) Relay
     * 11) Storage audit log settings ~ 3), 4)
     * 12) Batch log settings ~ 3), 5)
     * 13) Postgres log settings ~ 2), 3)
     * 14) AppInsights ~ 3)
     * */
    return List.of(/*Log analytics step, vNet step, */ );
  }
}
