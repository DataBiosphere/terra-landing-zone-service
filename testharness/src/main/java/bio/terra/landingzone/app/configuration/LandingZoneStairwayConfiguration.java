package bio.terra.landingzone.app.configuration;

import bio.terra.common.db.DataSourceInitializer;
import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.common.stairway.StairwayProperties;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.configuration.stairway.LandingZoneStairwayDatabaseConfiguration;
import bio.terra.landingzone.library.stairway.StairwayService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LandingZoneStairwayConfiguration {

  @Bean("landingZoneStairwayProperties")
  @ConfigurationProperties(prefix = "landingzone.stairway")
  public StairwayProperties getStairwayProperties() {
    return new StairwayProperties();
  }

  @Bean
  @ConfigurationProperties(prefix = "workspace.azure")
  public LandingZoneAzureConfiguration getLandingZoneAzureConfiguration() {
    return new LandingZoneAzureConfiguration();
  }

  @Bean("landingZoneStairwayOptionsBuilder")
  public StairwayComponent.StairwayOptionsBuilder getStairwayOptionsBuilder(
      LandingZoneStairwayDatabaseConfiguration landingZoneStairwayDatabaseConfiguration) {
    StairwayComponent.StairwayOptionsBuilder stairwayOptionsBuilder =
        new StairwayComponent.StairwayOptionsBuilder();
    return stairwayOptionsBuilder.dataSource(
        DataSourceInitializer.initializeDataSource(landingZoneStairwayDatabaseConfiguration));
  }

  @Bean("landingZoneStairwayComponent")
  public StairwayComponent getStairwayComponent(
      KubeService kubeService,
      KubeProperties kubeProperties,
      @Qualifier("landingZoneStairwayProperties") StairwayProperties stairwayProperties) {
    return new StairwayComponent(kubeService, kubeProperties, stairwayProperties);
  }

  @Bean("landingZoneStairwayService")
  public StairwayService getStairwayService(
      @Qualifier("landingZoneStairwayComponent") StairwayComponent stairwayComponent,
      @Qualifier("landingZoneStairwayOptionsBuilder")
          StairwayComponent.StairwayOptionsBuilder stairwayOptionsBuilder) {
    return new StairwayService(stairwayComponent, stairwayOptionsBuilder);
  }
}
