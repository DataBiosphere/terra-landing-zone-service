package bio.terra.landingzone.library.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "landingzone.stairway")
public class LandingZoneStairwayProperties {
  private boolean forceCleanStart;
  private boolean migrateUpgrade;
  private int maxParallelFlights;
  private Duration quietDownTimeout;
  private Duration terminateTimeout;
  private boolean tracingEnabled;
  private Duration retentionCheckInterval;
  private Duration completedFlightRetention;

  /**
   * clusterNameSuffix is used to generate names when creating pubsub queues. It must be null if the
   * topicId and subscriptionId are provided.
   */
  private String clusterNameSuffix;

  /**
   * PubSub topic to use for stairway work queue. It must exist in the current GCP project. It must
   * be null if clusterNameSuffix is provided.
   */
  private String gcpPubSubTopicId;

  /**
   * PubSub subscription to use for stairway work queue. It must exist in the current GCP project.
   * It must be null if clusterNameSuffix is provided.
   */
  private String gcpPubSubSubscriptionId;

  public boolean isForceCleanStart() {
    return forceCleanStart;
  }

  public void setForceCleanStart(boolean forceCleanStart) {
    this.forceCleanStart = forceCleanStart;
  }

  public boolean isMigrateUpgrade() {
    return migrateUpgrade;
  }

  public void setMigrateUpgrade(boolean migrateUpgrade) {
    this.migrateUpgrade = migrateUpgrade;
  }

  public int getMaxParallelFlights() {
    return maxParallelFlights;
  }

  public void setMaxParallelFlights(int maxParallelFlights) {
    this.maxParallelFlights = maxParallelFlights;
  }

  public Duration getQuietDownTimeout() {
    return quietDownTimeout;
  }

  public void setQuietDownTimeout(Duration quietDownTimeout) {
    this.quietDownTimeout = quietDownTimeout;
  }

  public Duration getTerminateTimeout() {
    return terminateTimeout;
  }

  public void setTerminateTimeout(Duration terminateTimeout) {
    this.terminateTimeout = terminateTimeout;
  }

  public boolean isTracingEnabled() {
    return tracingEnabled;
  }

  public void setTracingEnabled(boolean tracingEnabled) {
    this.tracingEnabled = tracingEnabled;
  }

  public String getClusterNameSuffix() {
    return clusterNameSuffix;
  }

  public void setClusterNameSuffix(String clusterNameSuffix) {
    this.clusterNameSuffix = clusterNameSuffix;
  }

  public Duration getRetentionCheckInterval() {
    return retentionCheckInterval;
  }

  public void setRetentionCheckInterval(Duration retentionCheckInterval) {
    this.retentionCheckInterval = retentionCheckInterval;
  }

  public Duration getCompletedFlightRetention() {
    return completedFlightRetention;
  }

  public void setCompletedFlightRetention(Duration completedFlightRetention) {
    this.completedFlightRetention = completedFlightRetention;
  }

  public String getGcpPubSubTopicId() {
    return gcpPubSubTopicId;
  }

  public void setGcpPubSubTopicId(String gcpPubSubTopicId) {
    this.gcpPubSubTopicId = gcpPubSubTopicId;
  }

  public String getGcpPubSubSubscriptionId() {
    return gcpPubSubSubscriptionId;
  }

  public void setGcpPubSubSubscriptionId(String gcpPubSubSubscriptionId) {
    this.gcpPubSubSubscriptionId = gcpPubSubSubscriptionId;
  }
}
