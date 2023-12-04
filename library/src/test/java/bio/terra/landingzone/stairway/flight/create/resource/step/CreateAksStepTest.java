package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepStatus;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.containerservice.fluent.models.ManagedClusterInner;
import com.azure.resourcemanager.containerservice.models.*;
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
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateAksStepTest extends BaseStepTest {
  private static final String RESOURCE_ID = "aksId";

  @Mock private KubernetesClusters mockKubernetesClusters;
  @Mock private KubernetesCluster mockKubernetesCluster;
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

  @Captor private ArgumentCaptor<Map<String, String>> tagsCaptor;
  @Captor private ArgumentCaptor<String> nodeResourceGroupCaptor;

  private CreateAksStep testStep;
  private String costSavingsSpotNodesEnabled = "false";
  private String costSavingsVpaEnabled = "false";

  @BeforeEach
  void setup() {
    testStep = new CreateAksStep(mockArmManagers, mockParametersResolver, mockResourceNameProvider);
    testStep.denySleepWhilePoolingForAksStatus();
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    String aksResourceName = "aksName";
    when(mockResourceNameProvider.getName(anyString())).thenReturn(aksResourceName);
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
    verifyAksNodeResourceGroupName(nodeResourceGroupCaptor.getValue(), mrg.name());
    verify(mockK8sDefinitionStageWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockK8sDefinitionStageWithCreate);
    verifyBasicTags(tagsCaptor.getValue(), LANDING_ZONE_ID);
    verifyOmsAgentAddonProfileNotSet();
  }

  @Test
  void testCostSavingSpotNodesInStep() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    String aksResourceName = "aksName";
    when(mockResourceNameProvider.getName(anyString())).thenReturn(aksResourceName);
    costSavingsSpotNodesEnabled = "true";

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
    setupCostSavingK8sMocks();

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    // verify cost saving tag
    assertTrue(
        tagsCaptor
            .getValue()
            .containsKey(LandingZoneTagKeys.AKS_COST_SAVING_SPOT_NODES_ENABLED.toString()));
    assertThat(
        tagsCaptor.getValue().get(LandingZoneTagKeys.AKS_COST_SAVING_SPOT_NODES_ENABLED.toString()),
        equalTo("true"));
  }

  @Test
  void testCostSavingVpaInStep() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    String aksResourceName = "aksName";
    when(mockResourceNameProvider.getName(anyString())).thenReturn(aksResourceName);
    costSavingsVpaEnabled = "true";

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
    setupCostSavingK8sMocks();
    setupVpaCostSavingK8sMocks();

    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    // verify cost saving tag
    assertTrue(
        tagsCaptor
            .getValue()
            .containsKey(LandingZoneTagKeys.AKS_COST_SAVING_VPA_ENABLED.toString()));
    assertThat(
        tagsCaptor.getValue().get(LandingZoneTagKeys.AKS_COST_SAVING_VPA_ENABLED.toString()),
        equalTo("true"));
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

  @ParameterizedTest
  @MethodSource("managedResourceGroupNameProvider")
  void validateNodeResourceGroupWithTruncation(
      String managedResourceGroupName, String expectedNodeResourceGroupName) {
    assertThat(
        testStep.getNodeResourceGroup(managedResourceGroupName),
        equalTo(expectedNodeResourceGroupName));
  }

  @Test
  void doStepRetryWhenAksIsNotProvisionedYet() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    String aksResourceName = "aksName";
    when(mockResourceNameProvider.getName(anyString())).thenReturn(aksResourceName);
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
    setupArmManagersForDoStepRetryWhenAksIsNotProvisionedYet();
    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockK8sDefinitionStageWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockK8sDefinitionStageWithCreate);
    // poll for aks 2 times based on mocks - 1st attempt failed, 2nd successful;
    // failed means that aks is not ready yet at the time of the request
    verify(mockKubernetesClusters, times(2)).getByResourceGroup(any(), any());
    verifyOmsAgentAddonProfileNotSet();
  }

  @Test
  void doStepRetryWhenAksIsAlreadyProvisioned() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    String aksResourceName = "aksName";
    when(mockResourceNameProvider.getName(anyString())).thenReturn(aksResourceName);
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
    setupArmManagersForDoStepRetryWhenAksIsAlreadyProvisioned();
    var stepResult = testStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    verify(mockK8sDefinitionStageWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockK8sDefinitionStageWithCreate);
    // aks has been already provisioned when we retry to call create
    verify(mockKubernetesClusters, times(1)).getByResourceGroup(any(), any());
    verifyOmsAgentAddonProfileNotSet();
  }

  private void verifyOmsAgentAddonProfileNotSet() {
    verify(mockK8sDefinitionStageWithCreate, never()).withAddOnProfiles(any());
  }

  private void verifyAksNodeResourceGroupName(String actualValue, String managedResourceGroup) {
    String expectedValue = "%s_aks".formatted(managedResourceGroup);
    assertThat(actualValue, equalTo(expectedValue));
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
            CromwellBaseResourcesFactory.ParametersNames.AKS_COST_SAVING_SPOT_NODES_ENABLED.name()))
        .thenReturn(costSavingsSpotNodesEnabled);
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_COST_SAVING_VPA_ENABLED.name()))
        .thenReturn(costSavingsVpaEnabled);
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_AAD_PROFILE_USER_GROUP_ID.name()))
        .thenReturn("00000000-0000-0000-0000-000000000000");
  }

  private void setupArmManagersForDoStep() {
    setupMocksForEnablingWorkloadIdentity(mockKubernetesCluster);
    when(mockKubernetesCluster.id()).thenReturn(RESOURCE_ID);
    when(mockK8sDefinitionStageWithCreate.create()).thenReturn(mockKubernetesCluster);

    setupK8sMocks();
  }

  private void setupK8sMocks() {
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
    when(mockK8sDefinitionStageWithCreate.withAgentPoolResourceGroup(
            nodeResourceGroupCaptor.capture()))
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

  private void setupCostSavingK8sMocks() {
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_SPOT_AUTOSCALING_MAX.name()))
        .thenReturn("10");
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.AKS_SPOT_MACHINE_TYPE.name()))
        .thenReturn(ContainerServiceVMSizeTypes.STANDARD_A2_V2.toString());
    KubernetesCluster.Update mockK8sUpdate = mock(KubernetesCluster.Update.class);
    when(mockKubernetesCluster.update()).thenReturn(mockK8sUpdate);
    var mockK8sAPDefinitionStagesBlank =
        mock(KubernetesClusterAgentPool.DefinitionStages.Blank.class);
    when(mockK8sUpdate.defineAgentPool("spotnodepool")).thenReturn(mockK8sAPDefinitionStagesBlank);

    var mockK8sAPDefinitionStagesMachineCount =
        mock(KubernetesClusterAgentPool.DefinitionStages.WithAgentPoolVirtualMachineCount.class);
    when(mockK8sAPDefinitionStagesBlank.withVirtualMachineSize(any()))
        .thenReturn(mockK8sAPDefinitionStagesMachineCount);

    var mockK8sAPDefinitionStagesWithAttach =
        mock(KubernetesClusterAgentPool.DefinitionStages.WithAttach.class);
    when(mockK8sAPDefinitionStagesMachineCount.withAgentPoolVirtualMachineCount(anyInt()))
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sAPDefinitionStagesWithAttach.withSpotPriorityVirtualMachine())
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sAPDefinitionStagesWithAttach.withAgentPoolMode(AgentPoolMode.USER))
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sAPDefinitionStagesWithAttach.withAutoScaling(anyInt(), anyInt()))
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sAPDefinitionStagesWithAttach.withVirtualNetwork(any(), any()))
        .thenReturn(mockK8sAPDefinitionStagesWithAttach);
    when(mockK8sAPDefinitionStagesWithAttach.attach()).thenReturn(mockK8sUpdate);
  }

  private void setupVpaCostSavingK8sMocks() {
    when(mockKubernetesCluster.innerModel()).thenReturn(mock(ManagedClusterInner.class));
    when(mockKubernetesCluster.innerModel().workloadAutoScalerProfile())
        .thenReturn(mock(ManagedClusterWorkloadAutoScalerProfile.class));
    when(mockKubernetesCluster
            .innerModel()
            .workloadAutoScalerProfile()
            .withVerticalPodAutoscaler(
                mock(ManagedClusterWorkloadAutoScalerProfileVerticalPodAutoscaler.class)
                    .withEnabled(true)))
        .thenReturn(mock(ManagedClusterWorkloadAutoScalerProfile.class));
  }

  // setup mocks for doStep retry attempt after Stairway failure
  private void setupArmManagersForDoStepRetryWhenAksIsNotProvisionedYet() {
    setupK8sMocks();

    /*throw exception during call to create aks*/
    mockManagementException("OperationNotAllowed");

    /*setup mocks for polling aks status*/
    var mockClusterNotReady = mock(KubernetesCluster.class);
    when(mockClusterNotReady.provisioningState()).thenReturn("provisioning");
    var mockClusterReady = mock(KubernetesCluster.class);
    when(mockClusterReady.provisioningState()).thenReturn("succeeded");
    when(mockKubernetesClusters.getByResourceGroup(any(), any()))
        .thenReturn(mockClusterNotReady, mockClusterReady);

    setupMocksForEnablingWorkloadIdentity(mockClusterReady);
  }

  private void setupArmManagersForDoStepRetryWhenAksIsAlreadyProvisioned() {
    setupK8sMocks();

    /*throw exception during call to create aks*/
    mockManagementException("Conflict");

    var mockClusterReady = mock(KubernetesCluster.class);
    when(mockKubernetesClusters.getByResourceGroup(any(), any())).thenReturn(mockClusterReady);

    setupMocksForEnablingWorkloadIdentity(mockClusterReady);
  }

  private void mockManagementException(String code) {
    var retryableConflictException = mock(ManagementException.class);
    var managementError = mock(ManagementError.class);
    when(managementError.getCode()).thenReturn(code);
    when(retryableConflictException.getValue()).thenReturn(managementError);
    var httpResponse = mock(HttpResponse.class);
    when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONFLICT.value());
    when(retryableConflictException.getResponse()).thenReturn(httpResponse);
    doThrow(retryableConflictException).when(mockK8sDefinitionStageWithCreate).create();
  }

  private void setupMocksForEnablingWorkloadIdentity(KubernetesCluster mockKubernetesCluster) {
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

  // provides pairs of mrg name and expected name of node resource group
  private static Stream<Arguments> managedResourceGroupNameProvider() {
    return Stream.of(
        Arguments.of("mrgNameWithoutTruncation", "mrgNameWithoutTruncation_aks"),
        Arguments.of("mrgName", "mrgName_aks"),
        Arguments.of("mrgMyCustomName", "mrgMyCustomName_aks"),
        Arguments.of(
            "mrgSuperSuperSuperSuperSuperLongManagedResourceGroupNameWhichShouldBeTruncatedImmediately",
            "mrgSuperSuperSuperSuperSuperLongManagedResourceGroupNameWhichShouldBeTruncat_aks"),
        Arguments.of(
            "mrgSuperSuperSuperSuperLongManagedResourceGroupNameWhichShouldBeTruncatedImmediately",
            "mrgSuperSuperSuperSuperLongManagedResourceGroupNameWhichShouldBeTruncatedImm_aks"),
        Arguments.of(
            "mrgSuperSuperSuperSuperLongManagedResourceGroupNameWhichMaybeMaybeMaybeMaybeMa",
            "mrgSuperSuperSuperSuperLongManagedResourceGroupNameWhichMaybeMaybeMaybeMaybe_aks"));
  }
}
