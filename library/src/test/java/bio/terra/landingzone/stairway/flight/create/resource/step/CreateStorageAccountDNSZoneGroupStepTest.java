package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.fluent.NetworkManagementClient;
import com.azure.resourcemanager.network.fluent.PrivateDnsZoneGroupsClient;
import com.azure.resourcemanager.network.fluent.models.PrivateDnsZoneGroupInner;
import com.azure.resourcemanager.network.models.PrivateEndpoints;
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
public class CreateStorageAccountDNSZoneGroupStepTest extends BaseStepTest {
  private CreateStorageAccountDNSZoneGroupStep testStep;
  @Mock private PrivateEndpoints mockPrivateEndpoints;
  @Mock private NetworkManager mockNetworkManager;
  @Mock private NetworkManagementClient mockNetworkManagementClient;
  @Mock private PrivateDnsZoneGroupsClient mockPrivateDnsZoneGroupsClient;
  @Mock private PrivateDnsZoneGroupInner mockPrivateDnsZoneGroupInner;

  @Captor private ArgumentCaptor<PrivateDnsZoneGroupInner> privateDnsZoneGroupCaptor;

  @BeforeEach
  void setup() {
    testStep = new CreateStorageAccountDNSZoneGroupStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String resourceName = UUID.randomUUID().toString();
    final String privateEndpointName = UUID.randomUUID().toString();
    final String privateDnsZoneGroupId = UUID.randomUUID().toString();
    final String privateDnsZoneResourceId = UUID.randomUUID().toString();

    when(mockResourceNameProvider.getName(testStep.getResourceType())).thenReturn(resourceName);
    when(mockPrivateDnsZoneGroupInner.id()).thenReturn(privateDnsZoneGroupId);

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
            CreateStorageAccountPrivateEndpointStep.STORAGE_ACCOUNT_PRIVATE_ENDPOINT_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(privateEndpointName).build(),
            CreateStorageAccountDNSZoneStep.STORAGE_ACCOUNT_DNS_ID,
            privateDnsZoneResourceId));
    setupArmManagersForDoStep(resourceName, privateEndpointName, mrg.name());

    // Run step
    var stepResult = testStep.doStep(mockFlightContext);

    // Verify step succeeded and populated the working map
    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(
                CreateStorageAccountDNSZoneGroupStep.STORAGE_ACCOUNT_DNS_ZONE_GROUP_ID,
                String.class),
        equalTo(privateDnsZoneGroupId));

    // Verify captors
    var zoneGroup = privateDnsZoneGroupCaptor.getValue();
    assertThat(zoneGroup.privateDnsZoneConfigs().size(), equalTo(1));
    assertThat(
        zoneGroup.privateDnsZoneConfigs().get(0).privateDnsZoneId(),
        equalTo(privateDnsZoneResourceId));
    assertThat(
        zoneGroup.privateDnsZoneConfigs().get(0).name(),
        equalTo("privatelink_blob_core_windows_net"));
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
    var mrg = ResourceStepFixture.createDefaultMrg();
    var privateEndpointName = UUID.randomUUID().toString();
    var zoneGroupName = UUID.randomUUID().toString();
    var resourceId =
        String.format(
            "/subscriptions/289871e8-b6d7-4b86-91b1-8d365496bf5c/resourceGroups/%s/providers/Microsoft.Network/privateEndpoints/%s/privateDnsZoneGroups/%s",
            mrg.name(), privateEndpointName, zoneGroupName);
    workingMap.put(
        CreateStorageAccountDNSZoneGroupStep.STORAGE_ACCOUNT_DNS_ZONE_GROUP_ID, resourceId);
    workingMap.put(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateEndpoints()).thenReturn(mockPrivateEndpoints);
    when(mockPrivateEndpoints.manager()).thenReturn(mockNetworkManager);
    when(mockNetworkManager.serviceClient()).thenReturn(mockNetworkManagementClient);
    when(mockNetworkManagementClient.getPrivateDnsZoneGroups())
        .thenReturn(mockPrivateDnsZoneGroupsClient);

    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockPrivateDnsZoneGroupsClient).delete(mrg.name(), privateEndpointName, zoneGroupName);
  }

  private void setupArmManagersForDoStep(
      String resourceName, String privateEndpointName, String mrg) {
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.privateEndpoints()).thenReturn(mockPrivateEndpoints);
    when(mockPrivateEndpoints.manager()).thenReturn(mockNetworkManager);
    when(mockNetworkManager.serviceClient()).thenReturn(mockNetworkManagementClient);
    when(mockNetworkManagementClient.getPrivateDnsZoneGroups())
        .thenReturn(mockPrivateDnsZoneGroupsClient);
    when(mockPrivateDnsZoneGroupsClient.createOrUpdate(
            eq(mrg),
            eq(privateEndpointName),
            eq(resourceName),
            privateDnsZoneGroupCaptor.capture()))
        .thenReturn(mockPrivateDnsZoneGroupInner);
  }
}
