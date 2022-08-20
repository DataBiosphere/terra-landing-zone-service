package bio.terra.landingzone.common.utils;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.library.AzureLandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.service.landingzone.azure.AzureLandingZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneFlightBeanBag {
  private final LandingZoneDao landingZoneDao;
  private LandingZoneAzureConfiguration azureConfiguration;
  private AzureLandingZoneService azureLandingZoneService;
  private AzureLandingZoneManagerProvider azureLandingZoneManagerProvider;

  @Autowired
  public LandingZoneFlightBeanBag(
      LandingZoneDao landingZoneDao,
      LandingZoneAzureConfiguration azureConfiguration,
      AzureLandingZoneService azureLandingZoneService,
      AzureLandingZoneManagerProvider azureLandingZoneManagerProvider) {
    this.landingZoneDao = landingZoneDao;
    this.azureConfiguration = azureConfiguration;
    this.azureLandingZoneService = azureLandingZoneService;
    this.azureLandingZoneManagerProvider = azureLandingZoneManagerProvider;
  }

  public LandingZoneDao getLandingZoneDao() {
    return landingZoneDao;
  }

  public LandingZoneAzureConfiguration getAzureConfiguration() {
    return azureConfiguration;
  }

  public AzureLandingZoneService getAzureLandingZoneService() {
    return azureLandingZoneService;
  }

  public AzureLandingZoneManagerProvider getAzureLandingZoneManagerProvider() {
    return azureLandingZoneManagerProvider;
  }

  public static LandingZoneFlightBeanBag getFromObject(Object object) {
    return (LandingZoneFlightBeanBag) object;
  }
}
