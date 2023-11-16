package bio.terra.lz.futureservice.common.fixture;

import bio.terra.lz.futureservice.generated.model.ApiSystemStatus;
import bio.terra.lz.futureservice.generated.model.ApiSystemStatusSystems;
import java.util.Map;

public class SystemStatusFixtures {
  private SystemStatusFixtures() {}

  public static ApiSystemStatus buildOKSystemStatus() {
    return buildWithCustomStatuses(true, true);
  }

  public static ApiSystemStatus buildSystemStatusWithSomeFailure() {
    return buildWithCustomStatuses(false, true);
  }

  public static ApiSystemStatus buildWithCustomStatuses(
      boolean databaseAvailable, boolean samAvailable) {
    var systems =
        Map.of(
            "Database",
            new ApiSystemStatusSystems().ok(databaseAvailable),
            "Sam",
            new ApiSystemStatusSystems().ok(samAvailable));
    return new ApiSystemStatus().ok(databaseAvailable && samAvailable).systems(systems);
  }
}
