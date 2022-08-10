package bio.terra.landingzone.library;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import bio.terra.landingzone.library.stairway.StairwayService;
import org.springframework.context.ApplicationContext;

/**
 * "Top" of the landing zone service handles initialization. I'm using a static for the dataSource
 * to avoid any sequencing confusion over autowiring.
 */
public class LandingZoneMain {
  private static final String CHANGELOG_PATH = "landingzonedb/changelog.xml";
  private static final String LANDING_ZONE_STAIRWAY_BEAN_NAME = "landingZoneStairway";
  private static final String LANDING_ZONE_STAIRWAY_OPTIONS_BUILDER_BEAN_NAME =
      "landingZoneStairwayOptionsBuilder";

  public static void initialize(
      ApplicationContext applicationContext, LiquibaseMigrator migrateService) {
    LandingZoneDatabaseConfiguration landingZoneDatabaseConfiguration =
        applicationContext.getBean(LandingZoneDatabaseConfiguration.class);

    if (landingZoneDatabaseConfiguration.isInitializeOnStart()) {
      migrateService.initialize(CHANGELOG_PATH, landingZoneDatabaseConfiguration.getDataSource());
    } else if (landingZoneDatabaseConfiguration.isUpgradeOnStart()) {
      migrateService.upgrade(CHANGELOG_PATH, landingZoneDatabaseConfiguration.getDataSource());
    }

    // initialize Landing Zone Stairway
    //    StairwayComponent stairwayComponent =
    //        applicationContext.getBean(LANDING_ZONE_STAIRWAY_BEAN_NAME, StairwayComponent.class);
    //    LandingZoneStairwayDatabaseConfiguration landingZoneStairwayDatabaseConfiguration =
    //            applicationContext.getBean(LandingZoneStairwayDatabaseConfiguration.class);

    // This lib isn't aware of WSM scope
    // Object flightBeanBag = applicationContext.getBean(FlightBeanBag.class); //WSM scope

    //    StairwayComponent.StairwayOptionsBuilder stairwayOptionsBuilder =
    //        applicationContext.getBean(LANDING_ZONE_STAIRWAY_OPTIONS_BUILDER_BEAN_NAME,
    // StairwayComponent.StairwayOptionsBuilder.class);

    StairwayService stairwayService =
        applicationContext.getBean("landingZoneStairwayService", StairwayService.class);
    stairwayService.initialize();
  }
}
