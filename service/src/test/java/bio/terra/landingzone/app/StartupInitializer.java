package bio.terra.landingzone.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.landingzone.library.LandingZoneMain;
import org.springframework.context.ApplicationContext;

public class StartupInitializer {
  private static final String CHANGELOG_PATH = "landingzonedb/changelog.xml";

  public static void initialize(ApplicationContext applicationContext) {
    // Initialize the Terra Landing Zone Service library
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);
    LandingZoneMain.initialize(applicationContext, migrateService);
  }
}
