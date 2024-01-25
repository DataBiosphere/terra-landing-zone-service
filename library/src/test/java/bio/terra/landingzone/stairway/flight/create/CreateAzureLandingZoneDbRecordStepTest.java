package bio.terra.landingzone.stairway.flight.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CreateAzureLandingZoneDbRecordStepTest {

  @Mock private LandingZoneDao landingZoneDao;
  @Mock private LandingZoneFlightBeanBag landingZoneFlightBeanBag;
  @Mock private FlightContext context;

  @BeforeEach
  public void setup() {
    when(landingZoneFlightBeanBag.getLandingZoneDao()).thenReturn(landingZoneDao);
    when(context.getApplicationContext()).thenReturn(landingZoneFlightBeanBag);
  }

  @Test
  public void doStepPersistsDBRecordWithCorrectFields() throws Exception {
    var expectedLandingZone =
        LandingZoneRecord.builder()
            .landingZoneId(UUID.randomUUID())
            .description("Definition:%s Version:%s")
            .displayName("test")
            .definition("test")
            .properties(Map.of())
            .resourceGroupId(UUID.randomUUID().toString())
            .tenantId(UUID.randomUUID().toString())
            .subscriptionId(UUID.randomUUID().toString())
            .region("test-region")
            .billingProfileId(UUID.randomUUID())
            .build();

    var landingZoneRequest =
        LandingZoneRequest.builder()
            .definition(expectedLandingZone.definition())
            .version(expectedLandingZone.version())
            .billingProfileId(expectedLandingZone.billingProfileId())
            .parameters(expectedLandingZone.properties())
            .build();
    var billingProfile =
        new ProfileModel()
            .id(expectedLandingZone.billingProfileId())
            .managedResourceGroupId(expectedLandingZone.resourceGroupId())
            .tenantId(UUID.fromString(expectedLandingZone.tenantId()))
            .subscriptionId(UUID.fromString(expectedLandingZone.subscriptionId()));
    var inputMap =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS, landingZoneRequest,
                LandingZoneFlightMapKeys.LANDING_ZONE_ID, expectedLandingZone.landingZoneId(),
                LandingZoneFlightMapKeys.BILLING_PROFILE, billingProfile));
    var workingMap =
        FlightTestUtils.prepareFlightWorkingParameters(
            Map.of(
                GetManagedResourceGroupInfo.TARGET_MRG_KEY,
                new TargetManagedResourceGroup(
                    expectedLandingZone.resourceGroupId(), expectedLandingZone.region())));
    when(context.getInputParameters()).thenReturn(inputMap);
    when(context.getWorkingMap()).thenReturn(workingMap);

    when(landingZoneDao.createLandingZone(any()))
        .thenAnswer(
            invocation -> {
              var landingZone = invocation.getArgument(0, LandingZoneRecord.class);
              assertThat(landingZone.displayName(), equalTo(expectedLandingZone.displayName()));
              assertThat(landingZone.properties(), equalTo(expectedLandingZone.properties()));
              assertThat(
                  landingZone.resourceGroupId(), equalTo(expectedLandingZone.resourceGroupId()));
              assertThat(landingZone.tenantId(), equalTo(expectedLandingZone.tenantId()));
              assertThat(
                  landingZone.subscriptionId(), equalTo(expectedLandingZone.subscriptionId()));
              assertThat(
                  landingZone.billingProfileId(), equalTo(expectedLandingZone.billingProfileId()));
              assertThat(landingZone.region(), equalTo(expectedLandingZone.region()));
              return landingZone.landingZoneId();
            });
    new CreateAzureLandingZoneDbRecordStep().doStep(context);
    verify(landingZoneDao).createLandingZone(any());
  }

  @Test
  public void undoStepDeletesLandingZoneDBRecord() throws Exception {
    var landingZoneId = UUID.randomUUID();
    var inputMap =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId));
    when(context.getInputParameters()).thenReturn(inputMap);
    when(landingZoneDao.deleteLandingZone(landingZoneId)).thenReturn(true);
    new CreateAzureLandingZoneDbRecordStep().undoStep(context);

    verify(landingZoneDao).deleteLandingZone(landingZoneId);
  }
}
