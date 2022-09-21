package bio.terra.landingzone.common.utils;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.service.bpm.BillingProfileManagerService;
import bio.terra.landingzone.service.iam.SamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneFlightBeanBag {
  private final LandingZoneDao landingZoneDao;
  private final LandingZoneAzureConfiguration azureConfiguration;
  private final LandingZoneManagerProvider landingZoneManagerProvider;
  private final SamService samService;
  private final BillingProfileManagerService bpmService;

  @Autowired
  public LandingZoneFlightBeanBag(
      LandingZoneDao landingZoneDao,
      LandingZoneAzureConfiguration azureConfiguration,
      LandingZoneManagerProvider landingZoneManagerProvider,
      SamService samService,
      BillingProfileManagerService bpmService) {
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

  public SamService getSamService() {
    return samService;
  }

  public BillingProfileManagerService getBpmService() {
    return bpmService;
  }

  public static LandingZoneFlightBeanBag getFromObject(Object object) {
    return (LandingZoneFlightBeanBag) object;
  }
}
