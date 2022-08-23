package bio.terra.landingzone.common.utils;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneFlightBeanBag {
  private final LandingZoneDao landingZoneDao;
  private LandingZoneAzureConfiguration azureConfiguration;
  private LandingZoneManagerProvider landingZoneManagerProvider;

  @Autowired
  public LandingZoneFlightBeanBag(
      LandingZoneDao landingZoneDao,
      LandingZoneAzureConfiguration azureConfiguration,
      LandingZoneManagerProvider landingZoneManagerProvider) {
    this.landingZoneDao = landingZoneDao;
    this.azureConfiguration = azureConfiguration;
    this.landingZoneManagerProvider = landingZoneManagerProvider;
  }

  public LandingZoneDao getLandingZoneDao() {
    return landingZoneDao;
  }

  public LandingZoneAzureConfiguration getAzureConfiguration() {
    return azureConfiguration;
  }

  public LandingZoneManagerProvider getAzureLandingZoneManagerProvider() {
    return landingZoneManagerProvider;
  }

  public static LandingZoneFlightBeanBag getFromObject(Object object) {
    return (LandingZoneFlightBeanBag) object;
  }
}
