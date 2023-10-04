package bio.terra.landingzone.common.utils;

import io.micrometer.core.instrument.Metrics;

public class MetricUtils {
  public static final String NAMESPACE = "landingzone";
  public static final String CLOUD_PLATFORM_TAG = "cloudPlatform";

  private MetricUtils() {}

  public static void incrementLandingZoneCreation(String type) {
    Metrics.globalRegistry
        .counter(
            String.format("%s.creation.count", NAMESPACE),
            CLOUD_PLATFORM_TAG,
            "AZURE",
            "TYPE",
            type)
        .increment();
  }
}
