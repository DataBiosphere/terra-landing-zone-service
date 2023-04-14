package bio.terra.landingzone.stairway.flight;

import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ProtectedDataStepsDefinitionProvider implements StepsDefinitionProvider{
    @Override
    public List<Pair<Step, RetryRule>> get() {
        return List.of();
    }
}
