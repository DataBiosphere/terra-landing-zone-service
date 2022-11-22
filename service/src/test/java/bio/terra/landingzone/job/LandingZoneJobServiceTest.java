package bio.terra.landingzone.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.ErrorReportException;
import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.job.exception.JobNotFoundException;
import bio.terra.landingzone.job.model.JobReport;
import bio.terra.landingzone.library.configuration.LandingZoneIngressConfiguration;
import bio.terra.landingzone.library.configuration.LandingZoneJobConfiguration;
import bio.terra.landingzone.library.configuration.stairway.LandingZoneStairwayDatabaseConfiguration;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.landingzone.service.iam.SamConstants;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.StartLandingZoneCreation;
import bio.terra.landingzone.stairway.common.utils.LandingZoneMdcHook;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.Stairway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LandingZoneJobServiceTest {

  public static final String EXCEPTION_MSG = "failed";
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
  void setUpFailedScenarioForRetrieveStartingAsyncJobResult() {
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
    var landingZoneRequest = createDefaultLandingZoneRequestBuilder().build();

    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class))
        .thenReturn(flightLandingZoneId);
    when(flightMap.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class))
        .thenReturn(landingZoneRequest);

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
  void verifyUserAccessForDeleteJobResult_jobIsRunningSamActionsAreChecked()
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();
    UUID flightLandingZoneId = landingZoneId;
    var landingZoneRequest = createDefaultLandingZoneRequestBuilder().build();

    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class))
        .thenReturn(flightLandingZoneId);
    when(flightMap.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class))
        .thenReturn(landingZoneRequest);

    landingZoneJobService.verifyUserAccessForDeleteJobResult(bearerToken, landingZoneId, jobId);

    verify(samService, times(1))
        .checkAuthz(
            bearerToken,
            SamConstants.SamResourceType.SPEND_PROFILE,
            landingZoneRequest.billingProfileId().toString(),
            SamConstants.SamSpendProfileAction.LINK);

    verify(samService, times(1))
        .checkAuthz(
            bearerToken,
            SamConstants.SamResourceType.LANDING_ZONE,
            landingZoneId.toString(),
            SamConstants.SamLandingZoneAction.DELETE);
  }

  @Test
  void verifyUserAccessForDeleteJobResult_jobIsRunningSamBillingProfileLinkActionFailed()
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();
    UUID flightLandingZoneId = landingZoneId;
    var landingZoneRequest = createDefaultLandingZoneRequestBuilder().build();

    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class))
        .thenReturn(flightLandingZoneId);
    when(flightMap.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class))
        .thenReturn(landingZoneRequest);

    doThrow(ForbiddenException.class)
        .when(samService)
        .checkAuthz(
            eq(bearerToken),
            eq(SamConstants.SamResourceType.SPEND_PROFILE),
            eq(landingZoneRequest.billingProfileId().toString()),
            eq(SamConstants.SamSpendProfileAction.LINK));

    assertThrows(
        ErrorReportException.class,
        () ->
            landingZoneJobService.verifyUserAccessForDeleteJobResult(
                bearerToken, landingZoneId, jobId));
  }

  @Test
  void verifyUserAccessForDeleteJobResult_jobIsRunningSamLandingZoneDeleteActionFailed()
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();
    UUID flightLandingZoneId = landingZoneId;
    var landingZoneRequest = createDefaultLandingZoneRequestBuilder().build();

    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightMap.get(LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class))
        .thenReturn(flightLandingZoneId);
    when(flightMap.get(
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, LandingZoneRequest.class))
        .thenReturn(landingZoneRequest);
    doNothing()
        .when(samService)
        .checkAuthz(
            eq(bearerToken),
            eq(SamConstants.SamResourceType.SPEND_PROFILE),
            eq(landingZoneRequest.billingProfileId().toString()),
            eq(SamConstants.SamSpendProfileAction.LINK));

    doThrow(ForbiddenException.class)
        .when(samService)
        .checkAuthz(
            eq(bearerToken),
            eq(SamConstants.SamResourceType.LANDING_ZONE),
            eq(landingZoneId.toString()),
            eq(SamConstants.SamLandingZoneAction.DELETE));

    assertThrows(
        ErrorReportException.class,
        () ->
            landingZoneJobService.verifyUserAccessForDeleteJobResult(
                bearerToken, landingZoneId, jobId));
  }

  @ParameterizedTest
  @EnumSource(
      value = FlightStatus.class,
      names = {"SUCCESS", "RUNNING"},
      mode = EnumSource.Mode.INCLUDE)
  void retrieveStartingAsyncJobResult_noErrorFlightState_returnsResult(FlightStatus flightStatus)
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();

    setUpNotFailedScenarioForRetrieveStartingAsyncJobResult(jobId, flightStatus);

    StartLandingZoneCreation result =
        new StartLandingZoneCreation(landingZoneId, "defintion", "version");

    var asyncResult = landingZoneJobService.retrieveStartingAsyncJobResult(jobId, result);

    assertThat(asyncResult.getResult(), equalTo(result));
    assertThat(asyncResult.getJobReport().getStatus(), is(notNullValue()));
    assertThat(asyncResult.getApiErrorReport(), is(nullValue()));
  }

  @ParameterizedTest
  @EnumSource(
      value = FlightStatus.class,
      names = {"ERROR", "FATAL"},
      mode = EnumSource.Mode.INCLUDE)
  void retrieveStartingAsyncJobResult_errorFlightState_returnsErrorReport(FlightStatus flightStatus)
      throws InterruptedException {
    String jobId = "myjob";
    UUID landingZoneId = UUID.randomUUID();

    setUpFailedScenarioForRetrieveStartingAsyncJobResult(jobId, flightStatus);

    StartLandingZoneCreation result =
        new StartLandingZoneCreation(landingZoneId, "defintion", "version");

    var asyncResult = landingZoneJobService.retrieveStartingAsyncJobResult(jobId, result);

    assertThat(asyncResult.getResult(), equalTo(result));
    assertThat(asyncResult.getJobReport().getStatus(), equalTo(JobReport.StatusEnum.FAILED));
    assertThat(asyncResult.getJobReport().getId(), equalTo(jobId));
    assertThat(asyncResult.getApiErrorReport().getMessage(), equalTo(EXCEPTION_MSG));
  }

  private void setUpFailedScenarioForRetrieveStartingAsyncJobResult(
      String jobId, FlightStatus flightStatus) throws InterruptedException {
    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    when(stairwayInstance.getFlightState(jobId)).thenReturn(flightState);
    when(flightState.getFlightStatus()).thenReturn(flightStatus);
    when(flightState.getInputParameters()).thenReturn(flightMap);
    when(flightState.getFlightId()).thenReturn(jobId);

    when(flightState.getSubmitted()).thenReturn(Instant.now());
    when(flightState.getCompleted()).thenReturn(Optional.of(Instant.now()));
    when(flightState.getException()).thenReturn(Optional.of(new RuntimeException(EXCEPTION_MSG)));

    when(flightMap.get(JobMapKeys.DESCRIPTION.getKeyName(), String.class))
        .thenReturn("Flight description");
    when(flightMap.get(JobMapKeys.RESULT_PATH.getKeyName(), String.class))
        .thenReturn("myresult-path");
    when(ingressConfig.getDomainName()).thenReturn("https://foo.com");
  }

  private void setUpNotFailedScenarioForRetrieveStartingAsyncJobResult(
      String jobId, FlightStatus flightStatus) throws InterruptedException {
    when(stairwayComponent.get()).thenReturn(stairwayInstance);
    FlightState scenarioFlightState = new FlightState();

    scenarioFlightState.setFlightStatus(flightStatus);
    scenarioFlightState.setInputParameters(flightMap);
    scenarioFlightState.setFlightId(jobId);
    scenarioFlightState.setCompleted(Instant.now());
    scenarioFlightState.setSubmitted(Instant.now());
    scenarioFlightState.setResultMap(new FlightMap());

    when(stairwayInstance.getFlightState(jobId)).thenReturn(scenarioFlightState);

    when(flightMap.get(JobMapKeys.DESCRIPTION.getKeyName(), String.class))
        .thenReturn("Flight description");
    when(flightMap.get(JobMapKeys.RESULT_PATH.getKeyName(), String.class))
        .thenReturn("myresult-path");
    when(ingressConfig.getDomainName()).thenReturn("https://foo.com");
  }

  /**
   * Initializes LandingZoneRequest.Builder with required parameters only. It can be used as is or
   * updated with other parameters to get custom version of LandingZoneRequest.
   *
   * <p>Customize with parameters:
   *
   * <p>var lzRequest = createDefaultLandingZoneRequestBuilder().version("newVersion").build();
   *
   * @return Instance of LandingZoneRequest.Builder
   */
  private LandingZoneRequest.Builder createDefaultLandingZoneRequestBuilder() {
    return LandingZoneRequest.builder()
        .definition("lzDefinition")
        .billingProfileId(UUID.randomUUID());
  }
}
