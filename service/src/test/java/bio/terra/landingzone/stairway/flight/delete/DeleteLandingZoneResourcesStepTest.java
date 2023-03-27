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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);
    when(flightContext.getInputParameters()).thenReturn(inputMap);
    when(flightContext.getWorkingMap()).thenReturn(workingMap);
  }

  @Test
  void doStep_deletesLandingZoneResources() throws LandingZoneRuleDeleteException {
    var landingZoneRecord = buildLandingZoneRecord(Collections.emptyMap());
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneRecord.landingZoneId());
    var deleteStep = new DeleteLandingZoneResourcesStep(landingZoneManagerProvider, landingZoneDao);
    when(landingZoneDao.getLandingZoneRecord(eq(landingZoneRecord.landingZoneId())))
        .thenReturn(landingZoneRecord);
    var deletedResources = List.of("deletedResource1", "deletedResource2");
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
  void doStep_doesNotDeleteResourcesForAttachedLandingZone() throws LandingZoneRuleDeleteException {
    var landingZoneRecord = buildLandingZoneRecord(Map.of(LandingZoneFlightMapKeys.ATTACH, "true"));
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneRecord.landingZoneId());
    when(landingZoneDao.getLandingZoneRecord(eq(landingZoneRecord.landingZoneId())))
        .thenReturn(landingZoneRecord);
    var deleteStep = new DeleteLandingZoneResourcesStep(landingZoneManagerProvider, landingZoneDao);

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

  private LandingZoneRecord buildLandingZoneRecord(Map<String, String> properties) {
    return new LandingZoneRecord(
        UUID.randomUUID(),
        "fake",
        "fake",
        "fake",
        "fake",
        "fake",
        UUID.randomUUID(),
        OffsetDateTime.now(),
        Optional.empty(),
        Optional.empty(),
        properties);
  }
}
