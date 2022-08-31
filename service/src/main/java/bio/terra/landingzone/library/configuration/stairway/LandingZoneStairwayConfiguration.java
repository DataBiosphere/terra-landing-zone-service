package bio.terra.landingzone.library.configuration.stairway;

import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.common.stairway.StairwayProperties;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.configuration.LandingZoneStairwayProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LandingZoneStairwayConfiguration {
  @Bean
  @ConfigurationProperties(prefix = "landingzone.stairway")
  public LandingZoneStairwayProperties getLandingZoneStairwayProperties() {
    return new LandingZoneStairwayProperties();
  }

  @Bean
  @ConfigurationProperties(prefix = "workspace.azure")
  public LandingZoneAzureConfiguration getLandingZoneAzureConfiguration() {
    return new LandingZoneAzureConfiguration();
  }

  @Bean("landingZoneStairwayComponent")
  public StairwayComponent getStairwayComponent(
      KubeService kubeService,
      KubeProperties kubeProperties,
      LandingZoneStairwayProperties landingZoneStairwayProperties) {
    StairwayProperties stairwayProperties = new StairwayProperties();
    stairwayProperties.setForceCleanStart(landingZoneStairwayProperties.isForceCleanStart());
    stairwayProperties.setMigrateUpgrade(landingZoneStairwayProperties.isMigrateUpgrade());
    stairwayProperties.setMaxParallelFlights(landingZoneStairwayProperties.getMaxParallelFlights());
    stairwayProperties.setQuietDownTimeout(landingZoneStairwayProperties.getQuietDownTimeout());
    stairwayProperties.setTerminateTimeout(landingZoneStairwayProperties.getTerminateTimeout());
    stairwayProperties.setTracingEnabled(landingZoneStairwayProperties.isTracingEnabled());
    stairwayProperties.setRetentionCheckInterval(
        landingZoneStairwayProperties.getRetentionCheckInterval());
    stairwayProperties.setCompletedFlightRetention(
        landingZoneStairwayProperties.getCompletedFlightRetention());
    return new StairwayComponent(kubeService, kubeProperties, stairwayProperties);
  }
}
