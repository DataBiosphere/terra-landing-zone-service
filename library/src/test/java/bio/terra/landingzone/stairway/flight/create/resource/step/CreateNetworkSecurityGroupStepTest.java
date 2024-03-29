package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.fluent.NetworkManagementClient;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup;
import com.azure.resourcemanager.network.models.NetworkSecurityGroups;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
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
class CreateNetworkSecurityGroupStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "networkSecurityGroupId";

  @Mock private NetworkManager mockNetworkManager;
  @Mock private NetworkManagementClient mockServiceClient;
  @Mock private NetworkSecurityGroup mockNsg;
  @Mock private NetworkSecurityGroups mockNsgs;

  @Mock private NetworkSecurityGroup.DefinitionStages.Blank mockNsgStageBlank;
  @Mock private NetworkSecurityGroup.DefinitionStages.WithGroup mockNsgWithGroup;
  @Mock private NetworkSecurityGroup.DefinitionStages.WithCreate mockNsgWithCreate;

  private CreateNetworkSecurityGroupStep createNetworkSecurityGroupStep;

  @Captor ArgumentCaptor<Map<String, String>> nsgTagsCaptor;

  @BeforeEach
  void setup() {
    createNetworkSecurityGroupStep =
        new CreateNetworkSecurityGroupStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String nsgName = "networkSecurityGroupName";

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(createNetworkSecurityGroupStep.getResourceType()))
        .thenReturn(nsgName);
    mockParametersResolver =
        new ParametersResolver(
            Map.of(
                LandingZoneDefaultParameters.ParametersNames.STORAGE_ACCOUNT_SKU_TYPE.name(),
                StorageAccountSkuType.STANDARD_LRS.name().toString()));
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            LandingZoneFlightMapKeys.CREATE_LANDING_ZONE_PARAMETERS_RESOLVER,
            mockParametersResolver));
    setupArmManagersForDoStep(nsgName, mrg.region(), mrg.name());

    var stepResult = createNetworkSecurityGroupStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verifyBasicTags(nsgTagsCaptor.getValue(), LANDING_ZONE_ID);
    verify(mockNsgWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockNsgWithCreate);
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createNetworkSecurityGroupStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    var stepResult = createNetworkSecurityGroupStep.undoStep(mockFlightContext);

    verify(mockNsgs, never()).deleteById(anyString());
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  private void setupArmManagersForDoStep(String name, String region, String resourceGroup) {
    when(mockNsg.id()).thenReturn(RESOURCE_ID);
    when(mockNsgWithCreate.create()).thenReturn(mockNsg);
    when(mockNsgWithCreate.withTags(nsgTagsCaptor.capture())).thenReturn(mockNsgWithCreate);
    when(mockNsgWithGroup.withExistingResourceGroup(resourceGroup)).thenReturn(mockNsgWithCreate);
    when(mockNsgStageBlank.withRegion(region)).thenReturn(mockNsgWithGroup);
    when(mockNsgs.define(name)).thenReturn(mockNsgStageBlank);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.networkSecurityGroups()).thenReturn(mockNsgs);
  }
}
