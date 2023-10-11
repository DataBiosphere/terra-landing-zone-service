package bio.terra.landingzone.common.utils;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;

public class MetricUtils {
  private static final String NAMESPACE = "landingzone";
  private static final String CLOUD_PLATFORM_TAG = "cloudPlatform";
  private static final String AZURE_PLATFORM_NAME = "AZURE";
  private static final String LANDINGZONE_TYPE_TAG = "type";
  private static final String LANDINGZONE_STEP_TAG = "step";

  // this is to set reasonable upper/lower bound for a step. postgres, aks take 7-8 minutes;
  // histogram metric would have buckets for all results.
  private static final int LANDINGZONE_STEP_MAX_DURATION_MILLISECONDS = 60 * 10 * 1000; // 10min
  private static final int LANDINGZONE_STEP_MIN_DURATION_MILLISECONDS = 60 * 1000; // 1min

  private MetricUtils() {}

  public static void incrementLandingZoneCreation(String type) {
    Metrics.globalRegistry
        .counter(
            String.format("%s.creation.count", NAMESPACE),
            CLOUD_PLATFORM_TAG,
            AZURE_PLATFORM_NAME,
            LANDINGZONE_TYPE_TAG,
            type)
        .increment();
  }

  public static void incrementLandingZoneCreationFailure(String type) {
    Metrics.globalRegistry
        .counter(
            String.format("%s.creation.failure.count", NAMESPACE),
            CLOUD_PLATFORM_TAG,
            AZURE_PLATFORM_NAME,
            LANDINGZONE_TYPE_TAG,
            type)
        .increment();
  }

  public static Timer configureTimerForLzStepDuration(String type, String step) {
    var registry = Metrics.globalRegistry;
    var t =
        Timer.builder(String.format("%s.step.latency", NAMESPACE))
            .description("Measure LZ step latency")
            .publishPercentileHistogram()
            .tags(
                CLOUD_PLATFORM_TAG,
                AZURE_PLATFORM_NAME,
                LANDINGZONE_TYPE_TAG,
                type,
                LANDINGZONE_STEP_TAG,
                step)
            .minimumExpectedValue(Duration.ofMillis(LANDINGZONE_STEP_MIN_DURATION_MILLISECONDS))
            .maximumExpectedValue(Duration.ofMillis(LANDINGZONE_STEP_MAX_DURATION_MILLISECONDS))
            .register(
                registry); // "register" method registers new or return existing one based on name
    // and tags
    return t;
  }
}
