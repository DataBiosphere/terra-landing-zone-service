package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.LandingZoneCreateException;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.BatchAccounts;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateBatchAccountStepTest extends BaseStepTest {
  private static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  private static final String BATCH_ACCOUNT_NAME = "testBatchAccount";
  private static final String BATCH_ACCOUNT_ID = "batchAccountId";

  @Mock private BatchManager mockBatchManager;
  @Mock private BatchAccounts mockBatchAccounts;
  @Mock private BatchAccount.DefinitionStages.Blank mockBatchAccountDefinitionStagesBlank;

  @Mock
  private BatchAccount.DefinitionStages.WithResourceGroup
      mockBatchAccountDefinitionStagesWithResourceGroup;

  @Mock private BatchAccount.DefinitionStages.WithCreate mockBatchAccountDefinitionStagesWithCreate;
  @Mock private BatchAccount mockBatchAccount;

  @Captor ArgumentCaptor<Map<String, String>> batchAccountTagsCaptor;

  private CreateBatchAccountStep createBatchAccountStep;

  @BeforeEach
  void setup() {
    createBatchAccountStep =
        new CreateBatchAccountStep(
            mockArmManagers, mockParametersResolver, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(createBatchAccountStep.getResourceType()))
        .thenReturn(BATCH_ACCOUNT_NAME);

    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg));
    setupArmManagersForDoStep(BATCH_ACCOUNT_ID, BATCH_ACCOUNT_NAME, mrg.region(), mrg.name());

    var stepResult = createBatchAccountStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyBasicTags(batchAccountTagsCaptor.getValue(), LANDING_ZONE_ID);
    verify(mockBatchAccountDefinitionStagesWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockBatchAccountDefinitionStagesWithCreate);
  }

  @Test
  void doStepWhenQuotaIssueThrown() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(createBatchAccountStep.getResourceType()))
        .thenReturn(BATCH_ACCOUNT_NAME);

    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg));
    setupMocks(BATCH_ACCOUNT_NAME, mrg.region(), mrg.name());
    var batchAccountQuotaException = mockBatchQuotaException();
    doThrow(batchAccountQuotaException).when(mockBatchAccountDefinitionStagesWithCreate).create();

    var stepResult = createBatchAccountStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_FATAL));
    assertTrue(stepResult.getException().isPresent());
    assertTrue(stepResult.getException().get() instanceof LandingZoneCreateException);
    assertNotNull(stepResult.getException().get().getCause());
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createBatchAccountStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    workingMap.put(CreateBatchAccountStep.BATCH_ACCOUNT_ID, BATCH_ACCOUNT_ID);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);
    when(mockBatchManager.batchAccounts()).thenReturn(mockBatchAccounts);
    when(mockArmManagers.batchManager()).thenReturn(mockBatchManager);

    var stepResult = createBatchAccountStep.undoStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockBatchAccounts, times(1)).deleteById(BATCH_ACCOUNT_ID);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    var workingMap = new FlightMap(); // empty, there is no BATCH_ACCOUNT_ID key
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    var stepResult = createBatchAccountStep.undoStep(mockFlightContext);

    verify(mockBatchAccounts, never()).deleteById(anyString());
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep(
      String batchAccountId, String name, String region, String resourceGroup) {
    when(mockBatchAccount.id()).thenReturn(batchAccountId);
    when(mockBatchAccountDefinitionStagesWithCreate.create()).thenReturn(mockBatchAccount);

    setupMocks(name, region, resourceGroup);
  }

  private void setupMocks(String name, String region, String resourceGroup) {
    when(mockBatchAccountDefinitionStagesWithCreate.withTags(batchAccountTagsCaptor.capture()))
        .thenReturn(mockBatchAccountDefinitionStagesWithCreate);
    when(mockBatchAccountDefinitionStagesWithResourceGroup.withExistingResourceGroup(resourceGroup))
        .thenReturn(mockBatchAccountDefinitionStagesWithCreate);
    when(mockBatchAccountDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockBatchAccountDefinitionStagesWithResourceGroup);
    when(mockBatchAccounts.define(name)).thenReturn(mockBatchAccountDefinitionStagesBlank);
    when(mockBatchManager.batchAccounts()).thenReturn(mockBatchAccounts);
    when(mockArmManagers.batchManager()).thenReturn(mockBatchManager);
  }

  private Exception mockBatchQuotaException() {
    var batchAccountQuotaException = mock(ManagementException.class);
    var managementError = mock(ManagementError.class);
    when(managementError.getCode()).thenReturn("code");
    when(managementError.getMessage())
        .thenReturn(
            "The regional Batch account quota for the specified subscription has been reached");
    when(batchAccountQuotaException.getValue()).thenReturn(managementError);
    when(batchAccountQuotaException.getMessage()).thenReturn("Polling failed with status code");
    return batchAccountQuotaException;
  }
}
