package bio.terra.landingzone.library;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * "Top" of the landing zone service handles initialization. I'm using a static for the dataSource
 * to avoid any sequencing confusion over autowiring.
 */
public class LandingZoneMain {
  private static final Logger logger = LoggerFactory.getLogger(LandingZoneMain.class);
  private static final String CHANGELOG_PATH = "landingzonedb/changelog.xml";

  public static void initialize(
      ApplicationContext applicationContext, LiquibaseMigrator migrateService) {
    logger.info("Initializing Landing Zone Service...");
    LandingZoneDatabaseConfiguration landingZoneDatabaseConfiguration =
        applicationContext.getBean(LandingZoneDatabaseConfiguration.class);

    if (landingZoneDatabaseConfiguration.isInitializeOnStart()) {
      migrateService.initialize(CHANGELOG_PATH, landingZoneDatabaseConfiguration.getDataSource());
    } else if (landingZoneDatabaseConfiguration.isUpgradeOnStart()) {
      migrateService.upgrade(CHANGELOG_PATH, landingZoneDatabaseConfiguration.getDataSource());
    }

    LandingZoneJobService landingZoneJobService =
        applicationContext.getBean("landingZoneJobService", LandingZoneJobService.class);
    landingZoneJobService.initialize();

    logger.info("Landing Zone Service initialized");
  }
}
