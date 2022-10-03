package bio.terra.landingzone.common.utils;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.service.bpm.LandingZoneBillingProfileManagerService;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneFlightBeanBag {
  private final LandingZoneDao landingZoneDao;
  private final LandingZoneAzureConfiguration azureConfiguration;
  private final LandingZoneManagerProvider landingZoneManagerProvider;
  private final LandingZoneSamService samService;
  private final LandingZoneBillingProfileManagerService bpmService;

  @Autowired
  public LandingZoneFlightBeanBag(
      LandingZoneDao landingZoneDao,
      LandingZoneAzureConfiguration azureConfiguration,
      LandingZoneManagerProvider landingZoneManagerProvider,
      LandingZoneSamService samService,
      LandingZoneBillingProfileManagerService bpmService) {
    this.landingZoneDao = landingZoneDao;
    this.azureConfiguration = azureConfiguration;
    this.landingZoneManagerProvider = landingZoneManagerProvider;
    this.samService = samService;
    this.bpmService = bpmService;
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

  public LandingZoneSamService getSamService() {
    return samService;
  }

  public LandingZoneBillingProfileManagerService getBpmService() {
    return bpmService;
  }

  public static LandingZoneFlightBeanBag getFromObject(Object object) {
    return (LandingZoneFlightBeanBag) object;
  }
}
