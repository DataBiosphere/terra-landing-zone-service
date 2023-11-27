package bio.terra.lz.futureservice.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.landingzone.library.LandingZoneMain;
import org.springframework.context.ApplicationContext;

public class StartupInitializer {
  public static void initialize(ApplicationContext applicationContext) {
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);

    // Initialize Terra Landing Zone library
    LandingZoneMain.initialize(applicationContext, migrateService);
  }
}
