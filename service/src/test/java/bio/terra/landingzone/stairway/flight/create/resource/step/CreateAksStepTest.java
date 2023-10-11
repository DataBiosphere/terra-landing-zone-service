package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepStatus;
import com.azure.resourcemanager.containerservice.fluent.models.ManagedClusterInner;
import com.azure.resourcemanager.containerservice.models.AgentPoolMode;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.containerservice.models.KubernetesClusterAgentPool;
import com.azure.resourcemanager.containerservice.models.KubernetesClusters;
import com.azure.resourcemanager.containerservice.models.ManagedClusterOidcIssuerProfile;
import com.azure.resourcemanager.containerservice.models.ManagedClusterSecurityProfile;
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
class CreateAksStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "aksId";

  @Mock private KubernetesClusters mockKubernetesClusters;
  @Mock private KubernetesCluster.DefinitionStages.Blank mockK8sDefinitionStageBlank;
  @Mock private KubernetesCluster.DefinitionStages.WithGroup mockK8sDefinitionStageWithGroup;
  @Mock private KubernetesCluster.DefinitionStages.WithVersion mockK8sDefinitionStageWithVersion;

  @Mock
  private KubernetesCluster.DefinitionStages.WithLinuxRootUsername
      mockK8sDefinitionStageWithLinuxRootUsername;

  @Mock private KubernetesCluster.DefinitionStages.WithCreate mockK8sDefinitionStageWithCreate;
  @Mock private KubernetesClusterAgentPool.DefinitionStages.Blank mockK8sAPDefinitionStagesBlank;

  @Mock
  private KubernetesClusterAgentPool.DefinitionStages.WithAgentPoolVirtualMachineCount
      mockK8sAPDefinitionStagesMachineCount;

  @Mock
  private KubernetesClusterAgentPool.DefinitionStages.WithAttach
      mockK8sAPDefinitionStagesWithAttach;

  @Mock private KubernetesCluster.Definition mockK8sDefinition;
  @Mock private KubernetesCluster mockKubernetesCluster;

  @Captor private ArgumentCaptor<Map<String, String>> tagsCaptor;

  private CreateAksStep testStep;

  @BeforeEach
  void setup() {
    testStep = new CreateAksStep(mockArmManagers, mockParametersResolver, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(anyString())).thenReturn("aksName");
    setupParameterResolver();
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
            CreateVnetStep.VNET_ID,
            "vNetId",
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_WORKSPACE_ID,
            "logAnalyticsWorkspaceId"));
    setupArmManagersForDoStep();

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockK8sDefinitionStageWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockK8sDefinitionStageWithCreate);
    verifyBasicTags(tagsCaptor.getValue(), LANDING_ZONE_ID);
    verifyOmsAgentAddonProfileNotSet();
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @ParameterizedTest
  @MethodSource("workingParametersProvider")
  void doStepMissingWorkingParameterThrowsException(Map<String, Object> workingParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(
            Map.of(
                LandingZoneFlightMapKeys.BILLING_PROFILE,
                new ProfileModel().id(UUID.randomUUID()),
                LandingZoneFlightMapKeys.LANDING_ZONE_ID,
                LANDING_ZONE_ID,
                LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
                ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()));
    FlightMap flightMapWorkingParameters =
        FlightTestUtils.prepareFlightWorkingParameters(workingParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);
    when(mockFlightContext.getWorkingMap()).thenReturn(flightMapWorkingParameters);

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  private void verifyOmsAgentAddonProfileNotSet() {
    verify(mockK8sDefinitionStageWithCreate, never()).withAddOnProfiles(any());
  }

  private void setupParameterResolver() {
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_MACHINE_TYPE.name()))
        .thenReturn(ContainerServiceVMSizeTypes.STANDARD_A2_V2.toString());
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_NODE_COUNT.name()))
        .thenReturn("1");
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_ENABLED.name()))
        .thenReturn("false");
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_AAD_PROFILE_USER_GROUP_ID.name()))
        .thenReturn("00000000-0000-0000-0000-000000000000");
  }

  private void setupArmManagersForDoStep() {
    setupMocksForEnablingWorkloadIdentity();
    when(mockKubernetesCluster.id()).thenReturn(RESOURCE_ID);
    when(mockK8sDefinitionStageWithCreate.create()).thenReturn(mockKubernetesCluster);
    when(mockK8sDefinitionStageWithCreate.withTags(tagsCaptor.capture()))
        .thenReturn(mockK8sDefinitionStageWithCreate);
    when(mockK8sDefinition.withDnsPrefix(anyString())).thenReturn(mockK8sDefinitionStageWithCreate);
    when(mockK8sAPDefinitionStagesWithAttach.attach()).thenReturn(mockK8sDefinition);
    when(mockK8sAPDefinitionStagesWithAttach.withVirtualNetwork(anyString(), anyString()))
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sDefinitionStageWithCreate.withAzureActiveDirectoryGroup(anyString()))
        .thenReturn(mockK8sDefinitionStageWithCreate);
    when(mockK8sAPDefinitionStagesWithAttach.withAgentPoolMode(eq(AgentPoolMode.SYSTEM)))
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sAPDefinitionStagesMachineCount.withAgentPoolVirtualMachineCount(anyInt()))
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sAPDefinitionStagesBlank.withVirtualMachineSize(
            any(ContainerServiceVMSizeTypes.class)))
        .thenReturn(mockK8sAPDefinitionStagesMachineCount);
    when(mockK8sDefinitionStageWithCreate.defineAgentPool(anyString()))
        .thenReturn(mockK8sAPDefinitionStagesBlank);
    when(mockK8sDefinitionStageWithLinuxRootUsername.withSystemAssignedManagedServiceIdentity())
        .thenReturn(mockK8sDefinitionStageWithCreate);
    when(mockK8sDefinitionStageWithVersion.withDefaultVersion())
        .thenReturn(mockK8sDefinitionStageWithLinuxRootUsername);
    when(mockK8sDefinitionStageWithGroup.withExistingResourceGroup(anyString()))
        .thenReturn(mockK8sDefinitionStageWithVersion);
    when(mockK8sDefinitionStageBlank.withRegion(anyString()))
        .thenReturn(mockK8sDefinitionStageWithGroup);
    when(mockKubernetesClusters.define(anyString())).thenReturn(mockK8sDefinitionStageBlank);
    when(mockAzureResourceManager.kubernetesClusters()).thenReturn(mockKubernetesClusters);
    when(mockArmManagers.azureResourceManager()).thenReturn(mockAzureResourceManager);
  }

  private void setupMocksForEnablingWorkloadIdentity() {
    KubernetesCluster.Update mockK8sUpdate = mock(KubernetesCluster.Update.class);
    ManagedClusterInner mockManagedClusterInner = mock(ManagedClusterInner.class);
    ManagedClusterSecurityProfile mockManagedClusterSecurityProfile =
        mock(ManagedClusterSecurityProfile.class);
    ManagedClusterOidcIssuerProfile mockManagedClusterOidcIssuerProfile =
        mock(ManagedClusterOidcIssuerProfile.class);
    when(mockKubernetesCluster.update()).thenReturn(mockK8sUpdate);
    when(mockManagedClusterInner.securityProfile()).thenReturn(mockManagedClusterSecurityProfile);
    when(mockManagedClusterOidcIssuerProfile.issuerUrl()).thenReturn("issuerUrl");
    when(mockManagedClusterInner.oidcIssuerProfile())
        .thenReturn(mockManagedClusterOidcIssuerProfile);
    when(mockKubernetesCluster.innerModel()).thenReturn(mockManagedClusterInner);
  }

  private static Stream<Arguments> workingParametersProvider() {
    return Stream.of(
        // intentionally return empty map, to check required parameter validation
        Arguments.of(Map.of()));
  }
}
