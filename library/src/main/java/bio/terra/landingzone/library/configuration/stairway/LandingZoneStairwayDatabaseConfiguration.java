package bio.terra.landingzone.library.configuration.stairway;

import bio.terra.common.db.BaseDatabaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "landingzone.landingzone-stairway-database")
public class LandingZoneStairwayDatabaseConfiguration extends BaseDatabaseProperties {
  /** Passed to Stairway, true will run the migrate to upgrade the database */
  private boolean migrateUpgrade;

  /**
   * Passed to Stairway, true will drop any existing stairway data and purge the work queue.
   * Otherwise existing flights are recovered.
   */
  private boolean forceClean;

  public boolean getMigrateUpgrade() {
    return migrateUpgrade;
  }

  public void setMigrateUpgrade(boolean migrateUpgrade) {
    this.migrateUpgrade = migrateUpgrade;
  }

  public boolean getForceClean() {
    return forceClean;
  }

  public void setForceClean(boolean forceClean) {
    this.forceClean = forceClean;
  }

  public boolean isMigrateUpgrade() {
    return migrateUpgrade;
  }

  public boolean isForceClean() {
    return forceClean;
  }
}
