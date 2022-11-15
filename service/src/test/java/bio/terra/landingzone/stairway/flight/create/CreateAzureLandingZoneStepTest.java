package bio.terra.landingzone.stairway.flight.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateAzureLandingZoneStepTest {

  private static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
  private static final String MRG_ID = "mymrg";
  private ProfileModel billingProfile;
  @Mock private CreateAzureLandingZoneStep createAzureLandingZoneStep;

  @Mock private LandingZoneManagerProvider landingZoneManagerProvider;
  @Mock private FlightContext flightContext;
  private FlightMap inputMap;

  private FlightMap workingMap;

  @Mock private LandingZoneManager landingZoneManager;

  @BeforeEach
  void setUp() {
    when(landingZoneManagerProvider.createLandingZoneManager(any())).thenReturn(landingZoneManager);
    createAzureLandingZoneStep = new CreateAzureLandingZoneStep(landingZoneManagerProvider);
    inputMap = new FlightMap();
    when(flightContext.getInputParameters()).thenReturn(inputMap);
    inputMap.put(LandingZoneFlightMapKeys.LANDING_ZONE_ID, LANDING_ZONE_ID.toString());
    workingMap = new FlightMap();
    billingProfile =
        new ProfileModel()
            .tenantId(TENANT_ID)
            .subscriptionId(SUBSCRIPTION_ID)
            .managedResourceGroupId(MRG_ID);
    workingMap.put(LandingZoneFlightMapKeys.BILLING_PROFILE, billingProfile);
    when(flightContext.getWorkingMap()).thenReturn(workingMap);
  }

  @Test
  void undoStep_deletesLandingZoneAndReturnsSuccess() throws LandingZoneRuleDeleteException {
    var result = createAzureLandingZoneStep.undoStep(flightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    verify(landingZoneManagerProvider, times(1))
        .createLandingZoneManager(LandingZoneTarget.fromBillingProfile(billingProfile));
    verify(landingZoneManager, times(1)).deleteResources(LANDING_ZONE_ID.toString());
  }

  @Test
  void undoStep_failedToDeleteReturnsFatalFailure() throws LandingZoneRuleDeleteException {

    when(landingZoneManager.deleteResources(LANDING_ZONE_ID.toString()))
        .thenThrow(LandingZoneRuleDeleteException.class);

    var result = createAzureLandingZoneStep.undoStep(flightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
    assertTrue(result.getException().isPresent());
  }
}
