package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateStorageAccountStepTest {
  private static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  private static final String STORAGE_ACCOUNT_ID = "storageAccountId";
  private static final String STORAGE_ACCOUNT_NAME = "testStorageAccount";

  @Mock private ArmManagers mockArmManagers;
  @Mock private ParametersResolver mockParametersResolver;
  @Mock private ResourceNameGenerator mockResourceNameGenerator;
  @Mock private FlightContext mockFlightContext;

  @Mock AzureResourceManager mockAzureResourceManager;
  @Mock StorageAccounts mockStorageAccounts;
  @Mock StorageAccount.DefinitionStages.Blank mockStorageAccountDefinitionStagesBlank;
  @Mock StorageAccount.DefinitionStages.WithGroup mockStorageAccountDefinitionStagesWithGroup;
  @Mock StorageAccount.DefinitionStages.WithCreate mockStorageAccountDefinitionStagesWithCreate;
  @Mock StorageAccount mockStorageAccount;

  @Captor ArgumentCaptor<Map<String, String>> storageAccountTagsCaptor;
  //  @Captor ArgumentCaptor<String> resourceGroupNameCaptor;
  //  @Captor ArgumentCaptor<String> resourceGroupRegionCaptor;

  private CreateStorageAccountStep createStorageAccountStep;

  @BeforeEach
  void setUp() {
    createStorageAccountStep =
        new CreateStorageAccountStep(
            mockArmManagers, mockParametersResolver, mockResourceNameGenerator);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    TargetManagedResourceGroup mrg = new TargetManagedResourceGroup("mgrName", "mrgRegion");
    when(mockResourceNameGenerator.nextName(ResourceNameGenerator.MAX_BATCH_ACCOUNT_NAME_LENGTH))
        .thenReturn(STORAGE_ACCOUNT_NAME);

    setupFlightContext(
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg));
    setupArmManagers(STORAGE_ACCOUNT_ID, STORAGE_ACCOUNT_NAME, mrg.region(), mrg.name());

    var stepResult = createStorageAccountStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyTags();
    verify(mockStorageAccountDefinitionStagesWithCreate, times(1)).create();
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createStorageAccountStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    workingMap.put(CreateStorageAccountStep.STORAGE_ACCOUNT_ID, STORAGE_ACCOUNT_ID);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);
    when(mockAzureResourceManager.storageAccounts()).thenReturn(mockStorageAccounts);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);

    var stepResult = createStorageAccountStep.undoStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockStorageAccounts, times(1)).deleteById(STORAGE_ACCOUNT_ID);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    var workingMap = new FlightMap(); // empty, there is no STORAGE_ACCOUNT_ID key
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    var stepResult = createStorageAccountStep.undoStep(mockFlightContext);

    verify(mockStorageAccounts, never()).deleteById(STORAGE_ACCOUNT_ID);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagers(
      String storageAccountId, String storageAccountName, String region, String resourceGroupName) {
    when(mockStorageAccount.id()).thenReturn(storageAccountId);
    when(mockStorageAccountDefinitionStagesWithCreate.create()).thenReturn(mockStorageAccount);
    when(mockStorageAccountDefinitionStagesWithCreate.withTags(storageAccountTagsCaptor.capture()))
        .thenReturn(mockStorageAccountDefinitionStagesWithCreate);
    when(mockStorageAccountDefinitionStagesWithGroup.withExistingResourceGroup(resourceGroupName))
        .thenReturn(mockStorageAccountDefinitionStagesWithCreate);
    when(mockStorageAccountDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockStorageAccountDefinitionStagesWithGroup);
    when(mockStorageAccounts.define(storageAccountName))
        .thenReturn(mockStorageAccountDefinitionStagesBlank);
    when(mockAzureResourceManager.storageAccounts()).thenReturn(mockStorageAccounts);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
  }

  private void setupFlightContext(
      Map<String, Object> inputParameters, Map<String, Object> workingMap) {
    FlightMap inputParamsMap = FlightTestUtils.prepareFlightInputParameters(inputParameters);
    FlightMap workingParamMap = FlightTestUtils.prepareFlightWorkingParameters(workingMap);
    when(mockFlightContext.getInputParameters()).thenReturn(inputParamsMap);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingParamMap);
  }

  private void verifyTags() {
    var numberOfExpectedTags = 2;
    var tags = storageAccountTagsCaptor.getValue();
    assertNotNull(tags);
    assertThat(tags.size(), equalTo(numberOfExpectedTags));
    assertTrue(tags.containsKey(LandingZoneTagKeys.LANDING_ZONE_ID.toString()));
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_ID.toString()),
        equalTo(LANDING_ZONE_ID.toString()));
    assertTrue(tags.containsKey(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()));
    assertThat(
        tags.get(LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString()),
        equalTo(ResourcePurpose.SHARED_RESOURCE.toString()));
  }

  static Stream<Arguments> inputParameterProvider() {
    return Stream.of(
        Arguments.of(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()))),
        Arguments.of(Map.of(LandingZoneFlightMapKeys.LANDING_ZONE_ID, LANDING_ZONE_ID)));
  }
}
