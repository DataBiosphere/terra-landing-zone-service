package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.Networks;
import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.network.models.PrivateEndpoints;
import com.azure.resourcemanager.network.models.Subnet;
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
class CreatePrivateEndpointStepTest extends BaseStepTest {
  private static final String PRIVATE_ENDPOINT_ID = "privateEndpointId";

  @Mock private Networks mockNetworks;
  @Mock Network mockNetwork;
  @Mock private Map<String, Subnet> mockSubnets;
  @Mock private Subnet mockSubnet;
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
      mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesBlank;

  @Mock
  private PrivateEndpoint.PrivateLinkServiceConnection.DefinitionStages.WithSubResource
      mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesWithSubResource;

  @Mock
  private PrivateEndpoint.PrivateLinkServiceConnection.DefinitionStages.WithAttach
      mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesWithAttach;

  @Mock
  private PrivateEndpoint.DefinitionStages.WithCreate mockPrivateEndpointDefinitionStagesWithCreate;

  @Mock private PrivateEndpoint mockPrivateEndpoint;

  @Captor private ArgumentCaptor<String> subnetIdCaptor;

  private CreatePrivateEndpointStep createPrivateEndpointStep;

  @BeforeEach
  void setup() {
    createPrivateEndpointStep =
        new CreatePrivateEndpointStep(
            mockArmManagers, mockParametersResolver, mockResourceNameGenerator);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    final String subnetId = "subnetId";
    final String endpointName = "endpointName";
    final String linkConnectionName = "linkConnectionName";
    final String postgresqlId = "postgresqlId";
    final String vnetId = "vnetId";

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreatePostgresqlDbStep.POSTGRESQL_ID,
            postgresqlId,
            CreateVnetStep.VNET_ID,
            vnetId));
    setupResourceNameGenerator(endpointName, linkConnectionName);
    setupArmManagersForDoStep(
        PRIVATE_ENDPOINT_ID,
        endpointName,
        vnetId,
        CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name(),
        subnetId,
        linkConnectionName,
        postgresqlId,
        mrg.region(),
        mrg.name());

    StepResult stepResult = createPrivateEndpointStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockPrivateEndpointDefinitionStagesWithCreate, times(1)).create();
    verify(mockNetworks, times(1)).getById(vnetId);
    verifyNoMoreInteractions(mockPrivateEndpointDefinitionStagesWithCreate);
    verifyNoMoreInteractions(mockNetworks);
    assertThat(subnetIdCaptor.getValue(), equalTo(subnetId));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createPrivateEndpointStep.doStep(mockFlightContext));
  }

  @ParameterizedTest
  @MethodSource("workingParameterProvider")
  void doStepMissingWorkingParameterThrowsException(Map<String, Object> workingParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()),
                LandingZoneFlightMapKeys.LANDING_ZONE_ID,
                LANDING_ZONE_ID));
    FlightMap flightMapWorkingParameters =
        FlightTestUtils.prepareFlightWorkingParameters(workingParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);
    when(mockFlightContext.getWorkingMap()).thenReturn(flightMapWorkingParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createPrivateEndpointStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    workingMap.put(CreatePrivateEndpointStep.PRIVATE_ENDPOINT_ID, PRIVATE_ENDPOINT_ID);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);
    when(mockAzureResourceManager.privateEndpoints()).thenReturn(mockPrivateEndpoints);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);

    var stepResult = createPrivateEndpointStep.undoStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockPrivateEndpoints, times(1)).deleteById(PRIVATE_ENDPOINT_ID);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    var workingMap = new FlightMap(); // empty, there is no PRIVATE_ENDPOINT_ID key
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    var stepResult = createPrivateEndpointStep.undoStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));

    verify(mockPrivateEndpoints, never()).deleteById(anyString());
  }

  private void setupArmManagersForDoStep(
      String endpointId,
      String endpointName,
      String vnetId,
      String subnetName,
      String subnetId,
      String privateLinkConnectionName,
      String privateLinkServiceResourceId,
      String region,
      String resourceGroup) {

    // mock query to get network info
    when(mockNetworks.getById(vnetId)).thenReturn(mockNetwork);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
    when(mockAzureResourceManager.networks()).thenReturn(mockNetworks);

    when(mockSubnet.id()).thenReturn(subnetId);
    when(mockSubnets.get(subnetName)).thenReturn(mockSubnet);
    when(mockNetwork.subnets()).thenReturn(mockSubnets);

    // mock creation of private endpoint
    when(mockPrivateEndpoint.id()).thenReturn(endpointId);
    when(mockPrivateEndpointDefinitionStagesWithCreate.create()).thenReturn(mockPrivateEndpoint);
    when(mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesWithAttach.attach())
        .thenReturn(mockPrivateEndpointDefinitionStagesWithCreate);
    when(mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesWithSubResource
            .withSubResource(any()))
        .thenReturn(mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesWithAttach);
    when(mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesBlank.withResourceId(
            privateLinkServiceResourceId))
        .thenReturn(mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesWithSubResource);
    when(mockPrivateEndpointDefinitionStagesWithPrivateLinkServiceConnection
            .definePrivateLinkServiceConnection(privateLinkConnectionName))
        .thenReturn(mockPrivateEndpointPrivateLinkServiceConnectionDefinitionStagesBlank);
    when(mockPrivateEndpointDefinitionStagesWithSubnet.withSubnetId(subnetIdCaptor.capture()))
        .thenReturn(mockPrivateEndpointDefinitionStagesWithPrivateLinkServiceConnection);
    when(mockPrivateEndpointDefinitionStagesWithGroup.withExistingResourceGroup(resourceGroup))
        .thenReturn(mockPrivateEndpointDefinitionStagesWithSubnet);
    when(mockPrivateEndpointDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockPrivateEndpointDefinitionStagesWithGroup);
    when(mockPrivateEndpoints.define(endpointName))
        .thenReturn(mockPrivateEndpointDefinitionStagesBlank);
    when(mockAzureResourceManager.privateEndpoints()).thenReturn(mockPrivateEndpoints);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
  }

  private void setupResourceNameGenerator(
      String privateEndpointName, String privateLinkConnectionName) {
    when(mockResourceNameGenerator.nextName(ResourceNameGenerator.MAX_PRIVATE_ENDPOINT_NAME_LENGTH))
        .thenReturn(privateEndpointName);
    when(mockResourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_PRIVATE_LINK_CONNECTION_NAME_LENGTH))
        .thenReturn(privateLinkConnectionName);
  }

  private static Stream<Arguments> workingParameterProvider() {
    return Stream.of(
        Arguments.of(Map.of(CreatePostgresqlDbStep.POSTGRESQL_ID, "postgreSqlId")),
        Arguments.of(Map.of(CreateVnetStep.VNET_ID, "vnetId")));
  }
}
