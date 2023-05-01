package bio.terra.landingzone.stairway.flight;

import bio.terra.stairway.FlightMap;
import java.util.Map;

public class FlightTestUtils {
  FlightTestUtils() {}

  public static FlightMap prepareFlightInputParameters(Map<String, Object> inputParameters) {
    FlightMap result = new FlightMap();
    inputParameters.forEach(result::put);
    return result;
  }

  public static FlightMap prepareFlightWorkingParameters(Map<String, Object> workingMap) {
    FlightMap result = new FlightMap();
    workingMap.forEach(result::put);
    return result;
  }
}
