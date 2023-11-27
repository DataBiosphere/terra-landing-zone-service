package bio.terra.lz.futureservice.app.configuration.spring;

import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import bio.terra.lz.futureservice.app.StartupInitializer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class BeanConfig {
  @Bean("jdbcTemplate")
  public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(
      LandingZoneDatabaseConfiguration config) {
    return new NamedParameterJdbcTemplate(config.getDataSource());
  }

  @Bean
  public SmartInitializingSingleton postSetupInitialization(ApplicationContext applicationContext) {
    return () -> StartupInitializer.initialize(applicationContext);
  }
}
