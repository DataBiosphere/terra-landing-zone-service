package bio.terra.lz.futureservice.app.configuration.spring;

import bio.terra.lz.futureservice.app.StartupInitializer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
  @Bean
  public SmartInitializingSingleton postSetupInitialization(ApplicationContext applicationContext) {
    return () -> StartupInitializer.initialize(applicationContext);
  }
}
