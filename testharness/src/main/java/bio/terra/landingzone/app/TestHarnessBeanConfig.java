package bio.terra.landingzone.app;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestHarnessBeanConfig {
  @Bean
  public SmartInitializingSingleton postSetupInitialization(ApplicationContext applicationContext) {
    return () -> TestHarnessStartupInitializer.initialize(applicationContext);
  }
}
