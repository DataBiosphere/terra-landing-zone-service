package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.network.models.PrivateEndpoints;
import com.azure.resourcemanager.network.models.PrivateLinkSubResourceName;
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
public class CreateStorageAccountPrivateEndpointStepTest extends BaseStepTest {
  private CreateStorageAccountPrivateEndpointStep testStep;
  @Mock private PrivateEndpoints mockPrivateEndpoints;
  @Mock private PrivateEndpoint.DefinitionStages.Blank mockPrivateEndpointDefinitionStagesBlank;

  @Mock
  private PrivateEndpoint.DefinitionStages.WithGroup mockPrivateEndpointDefinitionStagesWithGroup;

  @Mock
  private PrivateEndpoint.DefinitionStages.WithSubnet mockPrivateEndpointDefinitionStagesWithSubnet;

  @Mock
  private PrivateEndpoint.DefinitionStages.WithPrivateLinkServiceConnection
      mockPrivateEndpointDefinitionStagesWithPrivateLinkServiceConnection;

  @Mock
  private PrivateEndpoint.PrivateLinkServiceConnection.DefinitionStages.Blank
      mockPrivateLinkServiceConnectionDefinitionStagesBlank;

  @Mock
  private PrivateEndpoint.PrivateLinkServiceConnection.DefinitionStages.WithSubResource
      mockPrivateLinkServiceConnectionDefinitionStagesWithSubResource;

  @Mock
  private PrivateEndpoint.PrivateLinkServiceConnection.DefinitionStages.WithAttach
      mockPrivateLinkServiceConnectionDefinitionStagesWithAttach;

  @Mock
  private PrivateEndpoint.DefinitionStages.WithCreate mockPrivateEndpointDefinitionStagesWithCreate;

  @Mock private PrivateEndpoint mockPrivateEndpoint;

  @Captor private ArgumentCaptor<String> subnetCaptor;
  @Captor private ArgumentCaptor<String> storageAccountIdCaptor;
  @Captor private ArgumentCaptor<PrivateLinkSubResourceName> subResourceCaptor;

  @BeforeEach
  void setup() {
    testStep =
        new CreateStorageAccountPrivateEndpointStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String resourceName = UUID.randomUUID().toString();
    final String storageAccountId = UUID.randomUUID().toString();
    final String vnetId = UUID.randomUUID().toString();
    final String privateEndpointId = UUID.randomUUID().toString();

    when(mockResourceNameProvider.getName(testStep.getResourceType())).thenReturn(resourceName);
    when(mockPrivateEndpoint.id()).thenReturn(privateEndpointId);

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
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
            CreateStorageAccountStep.STORAGE_ACCOUNT_ID,
            storageAccountId,
            CreateVnetStep.VNET_ID,
            vnetId));
    setupArmManagersForDoStep(resourceName, mrg.region(), mrg.name());

    // Run step
    var stepResult = testStep.doStep(mockFlightContext);

    // Verify step succeeded and populated the working map
    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(
                CreateStorageAccountPrivateEndpointStep.STORAGE_ACCOUNT_PRIVATE_ENDPOINT_ID,
                String.class),
        equalTo(privateEndpointId));

    // Verify captors
    assertThat(subnetCaptor.getValue(), equalTo(vnetId + "/subnets/COMPUTE_SUBNET"));
    assertThat(storageAccountIdCaptor.getValue(), equalTo(storageAccountId));
    assertThat(subResourceCaptor.getValue(), equalTo(PrivateLinkSubResourceName.STORAGE_BLOB));

    // Verify create was called once
    verify(mockPrivateEndpointDefinitionStagesWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockPrivateEndpointDefinitionStagesWithCreate);
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    var resourceId = "resourceId";
    workingMap.put(
        CreateStorageAccountPrivateEndpointStep.STORAGE_ACCOUNT_PRIVATE_ENDPOINT_ID, resourceId);
    workingMap.put(
        GetManagedResourceGroupInfo.TARGET_MRG_KEY, ResourceStepFixture.createDefaultMrg());
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateEndpoints()).thenReturn(mockPrivateEndpoints);

    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockPrivateEndpoints).deleteById(resourceId);
  }

  private void setupArmManagersForDoStep(String privateEndpointName, String region, String mrg) {
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateEndpoints()).thenReturn(mockPrivateEndpoints);
    when(mockPrivateEndpoints.define(privateEndpointName))
        .thenReturn(mockPrivateEndpointDefinitionStagesBlank);
    when(mockPrivateEndpointDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockPrivateEndpointDefinitionStagesWithGroup);
    when(mockPrivateEndpointDefinitionStagesWithGroup.withExistingResourceGroup(mrg))
        .thenReturn(mockPrivateEndpointDefinitionStagesWithSubnet);
    when(mockPrivateEndpointDefinitionStagesWithSubnet.withSubnetId(subnetCaptor.capture()))
        .thenReturn(mockPrivateEndpointDefinitionStagesWithPrivateLinkServiceConnection);
    when(mockPrivateEndpointDefinitionStagesWithPrivateLinkServiceConnection
            .definePrivateLinkServiceConnection(privateEndpointName))
        .thenReturn(mockPrivateLinkServiceConnectionDefinitionStagesBlank);
    when(mockPrivateLinkServiceConnectionDefinitionStagesBlank.withResourceId(
            storageAccountIdCaptor.capture()))
        .thenReturn(mockPrivateLinkServiceConnectionDefinitionStagesWithSubResource);
    when(mockPrivateLinkServiceConnectionDefinitionStagesWithSubResource.withSubResource(
            subResourceCaptor.capture()))
        .thenReturn(mockPrivateLinkServiceConnectionDefinitionStagesWithAttach);
    when(mockPrivateLinkServiceConnectionDefinitionStagesWithAttach.attach())
        .thenReturn(mockPrivateEndpointDefinitionStagesWithCreate);
    when(mockPrivateEndpointDefinitionStagesWithCreate.create()).thenReturn(mockPrivateEndpoint);
  }
}
