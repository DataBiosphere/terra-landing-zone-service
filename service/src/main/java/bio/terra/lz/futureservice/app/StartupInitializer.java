package bio.terra.lz.futureservice.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.landingzone.library.LandingZoneMain;
import bio.terra.lz.futureservice.app.configuration.SentryConfiguration;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class StartupInitializer {
  private static final Logger logger = LoggerFactory.getLogger(StartupInitializer.class);

  public static void initialize(ApplicationContext applicationContext) {
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);
    SentryConfiguration sentryConfiguration = applicationContext.getBean(SentryConfiguration.class);

    // Initialize Terra Landing Zone library
    LandingZoneMain.initialize(applicationContext, migrateService);

    if (sentryConfiguration.dsn().isEmpty()) {
      logger.info("No Sentry DSN found. Starting up without it.");
    } else {
      Sentry.init(
          options -> {
            options.setDsn(sentryConfiguration.dsn());
            options.setEnvironment(sentryConfiguration.environment());
          });
      logger.info("Sentry DSN found. 5xx errors will be sent to Sentry.");
    }
  }
}
