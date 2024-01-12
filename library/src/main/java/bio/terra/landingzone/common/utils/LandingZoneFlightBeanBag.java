package bio.terra.landingzone.common.utils;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.library.AzureCredentialsProvider;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.AzureCustomerUsageConfiguration;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.configuration.LandingZoneTestingConfiguration;
import bio.terra.landingzone.service.bpm.LandingZoneBillingProfileManagerService;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneFlightBeanBag {
  private final LandingZoneService landingZoneService;
  private final LandingZoneDao landingZoneDao;
  private final LandingZoneTestingConfiguration testingConfiguration;
  private final LandingZoneManagerProvider landingZoneManagerProvider;
  private final LandingZoneSamService samService;
  private final LandingZoneBillingProfileManagerService bpmService;
  private final ObjectMapper objectMapper;
  private final LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration;
  private final ParametersResolverProvider parametersResolverProvider;
  private final AzureCustomerUsageConfiguration azureCustomerUsageConfiguration;
  private final AzureCredentialsProvider azureCredentialsProvider;

  @Lazy
  @Autowired
  public LandingZoneFlightBeanBag(
      LandingZoneService landingZoneService,
      LandingZoneDao landingZoneDao,
      LandingZoneTestingConfiguration testingConfiguration,
      LandingZoneManagerProvider landingZoneManagerProvider,
      LandingZoneSamService samService,
      LandingZoneBillingProfileManagerService bpmService,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration,
      ParametersResolverProvider parametersResolverProvider,
      AzureCustomerUsageConfiguration azureCustomerUsageConfiguration,
      AzureCredentialsProvider azureCredentialsProvider,
      ObjectMapper objectMapper) {
    this.landingZoneService = landingZoneService;
    this.landingZoneDao = landingZoneDao;
    this.testingConfiguration = testingConfiguration;
    this.landingZoneManagerProvider = landingZoneManagerProvider;
    this.samService = samService;
    this.bpmService = bpmService;
    this.landingZoneProtectedDataConfiguration = landingZoneProtectedDataConfiguration;
    this.parametersResolverProvider = parametersResolverProvider;
    this.azureCustomerUsageConfiguration = azureCustomerUsageConfiguration;
    this.azureCredentialsProvider = azureCredentialsProvider;
    this.objectMapper = objectMapper;
  }

  public LandingZoneService getLandingZoneService() {
    return landingZoneService;
  }

  public LandingZoneDao getLandingZoneDao() {
    return landingZoneDao;
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

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public LandingZoneTestingConfiguration getTestingConfiguration() {
    return testingConfiguration;
  }

  public LandingZoneProtectedDataConfiguration getLandingZoneProtectedDataConfiguration() {
    return landingZoneProtectedDataConfiguration;
  }

  public ParametersResolverProvider getParametersResolverProvider() {
    return parametersResolverProvider;
  }

  public AzureCustomerUsageConfiguration getAzureCustomerUsageConfiguration() {
    return azureCustomerUsageConfiguration;
  }

  public AzureCredentialsProvider getAzureCredentialsProvider() {
    return azureCredentialsProvider;
  }
}
