package bio.terra.landingzone.job;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.job.exception.JobNotFoundException;
import bio.terra.landingzone.library.configuration.LandingZoneIngressConfiguration;
import bio.terra.landingzone.library.configuration.LandingZoneJobConfiguration;
import bio.terra.landingzone.library.configuration.stairway.LandingZoneStairwayDatabaseConfiguration;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.landingzone.service.iam.SamConstants;
import bio.terra.landingzone.stairway.common.utils.LandingZoneMdcHook;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.Stairway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LandingZoneJobServiceTest {

  private LandingZoneJobService landingZoneJobService;
  @Mock private LandingZoneJobConfiguration jobConfig;
  @Mock private LandingZoneIngressConfiguration ingressConfig;
  @Mock private LandingZoneStairwayDatabaseConfiguration dbConfig;
  @Mock private LandingZoneMdcHook mdcHook;
  @Mock private StairwayComponent stairwayComponent;
  @Mock private LandingZoneFlightBeanBag flightBeanBag;
  @Mock private ObjectMapper mapper;
  @Mock private LandingZoneSamService samService;
  @Mock private Stairway stairwayInstance;

  @Mock private FlightState flightState;

  @Mock private FlightMap flightMap;

  @Mock private BearerToken bearerToken;

  @BeforeEach
  void setUp() {
    landingZoneJobService =
        new LandingZoneJobService(
            jobConfig,
            ingressConfig,
            dbConfig,
            mdcHook,
            stairwayComponent,
            flightBeanBag,
            mapper,
            samService);
  }

  @Test
  void verifyUserAccessForDeleteJobResult_jobSucceededValidSamUserIsChecked()
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();
    UUID flightLandingZoneId = landingZoneId;

    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightState.getFlightStatus()).thenReturn(FlightStatus.SUCCESS);
    when(flightMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class))
        .thenReturn(flightLandingZoneId);

    landingZoneJobService.verifyUserAccessForDeleteJobResult(bearerToken, landingZoneId, jobId);
  }

  @Test
  void verifyUserAccessForDeleteJobResult_landingZoneIdDoesNotMatchInIdFromJob()
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();
    UUID flightLandingZoneId = UUID.randomUUID();

    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class))
        .thenReturn(flightLandingZoneId);

    assertThrows(
        JobNotFoundException.class,
        () ->
            landingZoneJobService.verifyUserAccessForDeleteJobResult(
                bearerToken, landingZoneId, jobId));
  }

  @Test
  void verifyUserAccessForDeleteJobResult_jobIsRunningDeleteActionIsChecked()
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();
    UUID flightLandingZoneId = landingZoneId;

    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class))
        .thenReturn(flightLandingZoneId);
    when(flightState.getFlightStatus()).thenReturn(FlightStatus.RUNNING);

    landingZoneJobService.verifyUserAccessForDeleteJobResult(bearerToken, landingZoneId, jobId);

    verify(samService, times(1))
        .checkAuthz(
            bearerToken,
            SamConstants.SamResourceType.LANDING_ZONE,
            landingZoneId.toString(),
            SamConstants.SamLandingZoneAction.DELETE);
  }
}
