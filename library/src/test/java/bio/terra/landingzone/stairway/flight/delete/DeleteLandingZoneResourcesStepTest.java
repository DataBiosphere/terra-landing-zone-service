package bio.terra.landingzone.stairway.flight.delete;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.model.DeletedLandingZone;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class DeleteLandingZoneResourcesStepTest {

  @Mock private LandingZoneManagerProvider landingZoneManagerProvider;
  @Mock private LandingZoneDao landingZoneDao;
  @Mock private LandingZoneManager landingZoneManager;
  @Mock private FlightContext flightContext;
  private FlightMap workingMap;
  private FlightMap inputMap;

  @BeforeEach
  void setup() {
    workingMap = new FlightMap();
    inputMap = new FlightMap();
  }

  @Test
  void doStep_deletesLandingZoneResources()
      throws LandingZoneRuleDeleteException, InterruptedException {
    var landingZoneRecord = buildLandingZoneRecord(Collections.emptyMap());
    var deleteStep = new DeleteLandingZoneResourcesStep(landingZoneManagerProvider, landingZoneDao);
    var deletedResources = List.of("deletedResource1", "deletedResource2");
    when(flightContext.getInputParameters()).thenReturn(inputMap);
    when(flightContext.getWorkingMap()).thenReturn(workingMap);
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneRecord.landingZoneId());
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);
    when(landingZoneDao.getLandingZoneRecord(eq(landingZoneRecord.landingZoneId())))
        .thenReturn(landingZoneRecord);
    when(landingZoneManager.deleteResources(eq(landingZoneRecord.landingZoneId().toString())))
        .thenReturn(deletedResources);

    var result = deleteStep.doStep(flightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        workingMap.get(JobMapKeys.RESPONSE.getKeyName(), DeletedLandingZone.class),
        equalTo(
            new DeletedLandingZone(
                landingZoneRecord.landingZoneId(),
                deletedResources,
                landingZoneRecord.billingProfileId())));
    verify(landingZoneManager, times(1))
        .deleteResources(landingZoneRecord.landingZoneId().toString());
  }

  @Test
  void doStep_handlesInterrupted() throws LandingZoneRuleDeleteException, InterruptedException {
    var landingZoneRecord = buildLandingZoneRecord(Collections.emptyMap());
    var deleteStep = new DeleteLandingZoneResourcesStep(landingZoneManagerProvider, landingZoneDao);
    when(flightContext.getInputParameters()).thenReturn(inputMap);
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneRecord.landingZoneId());
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);
    when(landingZoneDao.getLandingZoneRecord(eq(landingZoneRecord.landingZoneId())))
        .thenReturn(landingZoneRecord);
    when(landingZoneManager.deleteResources(eq(landingZoneRecord.landingZoneId().toString())))
        .thenThrow(new RuntimeException("Interrupted", new InterruptedException()));

    Assertions.assertThrows(InterruptedException.class, () -> deleteStep.doStep(flightContext));
    assertThat(Thread.currentThread().isInterrupted(), equalTo(true));
  }

  @Test
  void doStep_deletesDbRecordWhenCloudResourcesAreInaccessible()
      throws LandingZoneRuleDeleteException, InterruptedException {
    var landingZoneRecord = buildLandingZoneRecord(Collections.emptyMap());
    var deleteStep = new DeleteLandingZoneResourcesStep(landingZoneManagerProvider, landingZoneDao);
    when(flightContext.getInputParameters()).thenReturn(inputMap);
    when(flightContext.getWorkingMap()).thenReturn(workingMap);
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneRecord.landingZoneId());
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenThrow(
            new ManagementException(
                "unauthed", null, new ManagementError("AuthorizationFailed", "unauthed")));
    when(landingZoneDao.getLandingZoneRecord(landingZoneRecord.landingZoneId()))
        .thenReturn(landingZoneRecord);

    var result = deleteStep.doStep(flightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        workingMap.get(JobMapKeys.RESPONSE.getKeyName(), DeletedLandingZone.class),
        equalTo(
            new DeletedLandingZone(
                landingZoneRecord.landingZoneId(),
                Collections.emptyList(),
                landingZoneRecord.billingProfileId())));
    verify(landingZoneManager, times(0))
        .deleteResources(landingZoneRecord.landingZoneId().toString());
  }

  @Test
  void doStep_doesNotDeleteResourcesForAttachedLandingZone()
      throws LandingZoneRuleDeleteException, InterruptedException {
    var landingZoneRecord = buildLandingZoneRecord(Map.of(LandingZoneFlightMapKeys.ATTACH, "true"));
    var deleteStep = new DeleteLandingZoneResourcesStep(landingZoneManagerProvider, landingZoneDao);
    when(flightContext.getInputParameters()).thenReturn(inputMap);
    when(flightContext.getWorkingMap()).thenReturn(workingMap);
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneRecord.landingZoneId());
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);
    when(landingZoneDao.getLandingZoneRecord(eq(landingZoneRecord.landingZoneId())))
        .thenReturn(landingZoneRecord);

    var result = deleteStep.doStep(flightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        workingMap.get(JobMapKeys.RESPONSE.getKeyName(), DeletedLandingZone.class),
        equalTo(
            new DeletedLandingZone(
                landingZoneRecord.landingZoneId(),
                Collections.emptyList(),
                landingZoneRecord.billingProfileId())));
    verify(landingZoneManager, times(0)).deleteResources(anyString());
  }

  @Test
  void doStep_succeedsWhenNoLandingZoneRecord() throws InterruptedException {
    var lzId = UUID.randomUUID();
    var deleteStep = new DeleteLandingZoneResourcesStep(landingZoneManagerProvider, landingZoneDao);
    when(flightContext.getInputParameters()).thenReturn(inputMap);
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, lzId);
    when(landingZoneDao.getLandingZoneIfExists(lzId)).thenReturn(Optional.empty());

    var result = deleteStep.doStep(flightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
  }

  private LandingZoneRecord buildLandingZoneRecord(Map<String, String> properties) {
    return new LandingZoneRecord(
        UUID.randomUUID(),
        "fake",
        "fake",
        "fake",
        "fake",
        "fake",
        UUID.randomUUID(),
        null,
        OffsetDateTime.now(),
        Optional.empty(),
        Optional.empty(),
        properties);
  }
}
