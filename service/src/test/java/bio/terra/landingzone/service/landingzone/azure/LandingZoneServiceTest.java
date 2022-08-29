package bio.terra.landingzone.service.landingzone.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.job.LandingZoneJobBuilder;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.DeployedVNet;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.ResourcesReader;
import bio.terra.landingzone.model.AzureCloudContext;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResourcesByPurpose;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneSubnetResource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LandingZoneServiceTest {
  private static final String VNET_1 = "vnet_1";
  private static final String VNET_2 = "vnet_2";
  private static final String VNET_3 = "vnet_3";
  private static final String VNET_SUBNET_1 = "vnet_subnet_1";
  private static final String VNET_SUBNET_2 = "vnet_subnet_2";
  private static final String VNET_SUBNET_3 = "vnet_subnet_3";
  private static final String STORAGE_ACCOUNT_1 = "StorageAccount_1";
  private static final String STORAGE_ACCOUNT_2 = "StorageAccount_2";
  private static final String VIRTUAL_NETWORK = "VirtualNetwork";
  private static final String STORAGE_ACCOUNT = "StorageAccount";
  private static final String SUBNET = "Subnet";
  private static final String REGION = "westus";

  private LandingZoneService landingZoneService;

  @Mock private LandingZoneManager landingZoneManager;

  @Mock private LandingZoneJobService landingZoneJobService;

  @Captor ArgumentCaptor<String> jobIdCaptor;
  @Captor ArgumentCaptor<Class<?>> classCaptor;

  @Mock private AzureCloudContext azureCloudContext;

  @Mock private LandingZoneManagerProvider landingZoneManagerProvider;

  @BeforeEach
  public void setup() {
    landingZoneService = new LandingZoneService(landingZoneJobService, landingZoneManagerProvider);
  }

  @Test
  public void getAsyncJobResult_success() {
    String jobId = "newJobId";
    landingZoneService.getAsyncJobResult(jobId);

    verify(landingZoneJobService, times(1))
        .retrieveAsyncJobResult(jobIdCaptor.capture(), classCaptor.capture());
    assertEquals(jobId, jobIdCaptor.getValue());
    assertEquals(DeployedLandingZone.class, classCaptor.getValue());
  }

  @Test
  public void startLandingZoneCreationJob_JobIsSubmitted() {
    setupAzureCloudContextMock("tenantId", "subscriptionId", "resourceGroupId");

    var mockFactory1 = mock(LandingZoneDefinitionFactory.class);
    when(mockFactory1.availableVersions())
        .thenReturn(List.of(DefinitionVersion.V1, DefinitionVersion.V2));

    List<FactoryDefinitionInfo> factories =
        List.of(
            new FactoryDefinitionInfo(
                mockFactory1.getClass().getName(),
                "mockFactory",
                mockFactory1.getClass().getName(),
                mockFactory1.availableVersions()));

    LandingZoneJobBuilder mockJobBuilder = mock(LandingZoneJobBuilder.class);
    when(mockJobBuilder.jobId(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.description(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.flightClass(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.landingZoneRequest(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.operationType(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.addParameter(any(), any())).thenReturn(mockJobBuilder);
    when(landingZoneJobService.newJob()).thenReturn(mockJobBuilder);

    try (MockedStatic<LandingZoneManager> staticMockLandingZoneManager =
        Mockito.mockStatic(LandingZoneManager.class)) {
      staticMockLandingZoneManager
          .when(LandingZoneManager::listDefinitionFactories)
          .thenReturn(factories);

      LandingZoneRequest landingZoneRequest =
          LandingZoneRequest.builder()
              .definition(mockFactory1.getClass().getName())
              .version(DefinitionVersion.V1.toString())
              .parameters(null)
              .azureCloudContext(azureCloudContext)
              .build();
      landingZoneService.startLandingZoneCreationJob(
          "newJobId", landingZoneRequest, "create-result");
    }

    verify(landingZoneJobService, times(1)).newJob();
    verify(mockJobBuilder, times(1)).submit();
  }

  @Test
  public void startLandingZoneCreationJob_ThrowsErrorWhenDefinitionDoesntExist() {
    setupAzureCloudContextMock("tenantId", "subscriptionId", "resourceGroupId");

    var mockFactory1 = mock(LandingZoneDefinitionFactory.class);
    when(mockFactory1.availableVersions())
        .thenReturn(List.of(DefinitionVersion.V1, DefinitionVersion.V2));

    List<FactoryDefinitionInfo> factories =
        List.of(
            new FactoryDefinitionInfo(
                mockFactory1.getClass().getName(),
                "mockFactory",
                mockFactory1.getClass().getName(),
                mockFactory1.availableVersions()));

    try (MockedStatic<LandingZoneManager> staticMockLandingZoneManager =
        Mockito.mockStatic(LandingZoneManager.class)) {
      staticMockLandingZoneManager
          .when(LandingZoneManager::listDefinitionFactories)
          .thenReturn(factories);

      LandingZoneRequest landingZoneRequest =
          LandingZoneRequest.builder()
              .definition("NotExistingDefinition")
              .version(DefinitionVersion.V5.toString())
              .parameters(null)
              .azureCloudContext(azureCloudContext)
              .build();
      Assertions.assertThrows(
          LandingZoneDefinitionNotFound.class,
          () ->
              landingZoneService.startLandingZoneCreationJob(
                  "jobId", landingZoneRequest, "create-result"));
    }
  }

  @Test
  public void listResourcesByPurpose_Success() {
    var purposeTags =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.SHARED_RESOURCE.toString());
    var deployedResources =
        List.of(
            new DeployedResource(VNET_1, VIRTUAL_NETWORK, purposeTags, REGION),
            new DeployedResource(VNET_SUBNET_1, SUBNET, purposeTags, REGION));

    when(landingZoneManagerProvider.createLandingZoneManager(azureCloudContext))
        .thenReturn(landingZoneManager);

    ResourcesReader resourceReader = mock(ResourcesReader.class);
    when(resourceReader.listResourcesByPurpose(ArgumentMatchers.any()))
        .thenReturn(deployedResources);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    List<LandingZoneResource> resources =
        landingZoneService.listResourcesByPurpose(
            ResourcePurpose.SHARED_RESOURCE, azureCloudContext);

    assertNotNull(resources);
    assertEquals(2, resources.size());
  }

  @Test
  public void listLandingZoneDefinitions_Success() {
    var mockFactory1 = mock(LandingZoneDefinitionFactory.class);

    List<FactoryDefinitionInfo> factories =
        List.of(
            new FactoryDefinitionInfo(
                "name1",
                "description1",
                mockFactory1.getClass().getName(),
                List.of(DefinitionVersion.V1, DefinitionVersion.V2)));
    try (MockedStatic<LandingZoneManager> staticMockLandingZoneManager =
        Mockito.mockStatic(LandingZoneManager.class)) {
      staticMockLandingZoneManager
          .when(LandingZoneManager::listDefinitionFactories)
          .thenReturn(factories);
      List<LandingZoneDefinition> templates = landingZoneService.listLandingZoneDefinitions();

      assertEquals(
          1,
          countAzureLandingZoneTemplateRecordsWithAttribute(
              templates, "name1", "description1", mockFactory1.getClass().getName(), "v1"));
      assertEquals(
          1,
          countAzureLandingZoneTemplateRecordsWithAttribute(
              templates, "name1", "description1", mockFactory1.getClass().getName(), "v2"));
    }
  }

  @Test
  public void deleteAzureLandingZone_ThrowsException() {
    Assertions.assertThrows(
        LandingZoneDeleteNotImplemented.class,
        () -> landingZoneService.deleteLandingZone("lz-1"),
        "Delete operation is not supported");
  }

  @Test
  public void listGeneralResourcesWithPurposes_Success() {
    var purposeTagSet1 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.SHARED_RESOURCE.toString());
    var purposeTagSet2 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.WLZ_RESOURCE.toString());
    var deployedResources =
        List.of(
            new DeployedResource(STORAGE_ACCOUNT_1, STORAGE_ACCOUNT, purposeTagSet1, REGION),
            new DeployedResource(VNET_2, VIRTUAL_NETWORK, purposeTagSet2, REGION),
            new DeployedResource(STORAGE_ACCOUNT_2, STORAGE_ACCOUNT, purposeTagSet1, REGION));

    when(landingZoneManagerProvider.createLandingZoneManager(azureCloudContext))
        .thenReturn(landingZoneManager);
    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(resourceReader.listResources()).thenReturn(deployedResources);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    var result = landingZoneService.listResourcesWithPurposes(azureCloudContext);
    assertNotNull(result);

    Map<String, List<LandingZoneResource>> resourcesGrouped = result.generalResources();
    assertNotNull(resourcesGrouped);

    Map<String, List<LandingZoneSubnetResource>> subnetResourcesGrouped = result.subnetResources();
    assertNotNull(subnetResourcesGrouped);

    // Validate number of purpose groups returned: two groups expected
    assertEquals(2, resourcesGrouped.size());
    assertEquals(0, subnetResourcesGrouped.size());
    assertTrue(resourcesGrouped.containsKey(ResourcePurpose.SHARED_RESOURCE.toString()));
    assertTrue(resourcesGrouped.containsKey(ResourcePurpose.WLZ_RESOURCE.toString()));
    // Validate number of members in each group
    assertEquals(2, resourcesGrouped.get(ResourcePurpose.SHARED_RESOURCE.toString()).size());
    assertEquals(1, resourcesGrouped.get(ResourcePurpose.WLZ_RESOURCE.toString()).size());
  }

  @Test
  public void listSubnetResourcesWithPurposes_Success() {
    HashMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap1 = new HashMap<>();
    HashMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap2 = new HashMap<>();
    subnetHashMap1.put(
        SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET,
        new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_1));
    subnetHashMap2.put(
        SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET,
        new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_2));
    subnetHashMap2.put(
        SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET,
        new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_3));

    var deployedVNetResources =
        List.of(
            new DeployedVNet(VNET_3, subnetHashMap2, REGION),
            new DeployedVNet(VNET_1, subnetHashMap1, REGION));

    when(landingZoneManagerProvider.createLandingZoneManager(azureCloudContext))
        .thenReturn(landingZoneManager);
    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(resourceReader.listVNets()).thenReturn(deployedVNetResources);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    var result = landingZoneService.listResourcesWithPurposes(azureCloudContext);
    assertNotNull(result);

    Map<String, List<LandingZoneResource>> resourcesGrouped = result.generalResources();
    assertNotNull(resourcesGrouped);

    Map<String, List<LandingZoneSubnetResource>> subnetResourcesGrouped = result.subnetResources();
    assertNotNull(subnetResourcesGrouped);

    assertEquals(0, resourcesGrouped.size(), "No general resources groups expected");
    assertEquals(2, subnetResourcesGrouped.size(), "Two subnet resources groups expected");
    assertTrue(
        subnetResourcesGrouped.containsKey(
            SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString()));
    assertTrue(
        subnetResourcesGrouped.containsKey(
            SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET.toString()));
    assertEquals(
        2,
        subnetResourcesGrouped
            .get(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString())
            .size());
    assertEquals(
        1,
        subnetResourcesGrouped
            .get(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET.toString())
            .size());
  }

  @Test
  public void listResourcesWithPurposes_Success() {
    HashMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap1 = new HashMap<>();
    HashMap<SubnetResourcePurpose, DeployedSubnet> subnetHashMap2 = new HashMap<>();
    var purposeTagSet1 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.SHARED_RESOURCE.toString());
    var purposeTagSet2 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.WLZ_RESOURCE.toString());
    var deployedResources =
        List.of(
            new DeployedResource(STORAGE_ACCOUNT_1, STORAGE_ACCOUNT, purposeTagSet1, REGION),
            new DeployedResource(VNET_2, VIRTUAL_NETWORK, purposeTagSet2, REGION),
            new DeployedResource(STORAGE_ACCOUNT_2, STORAGE_ACCOUNT, purposeTagSet1, REGION));

    subnetHashMap1.put(
        SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET,
        new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_1));
    subnetHashMap2.put(
        SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET,
        new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_2));
    subnetHashMap2.put(
        SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET,
        new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_3));

    when(landingZoneManagerProvider.createLandingZoneManager(azureCloudContext))
        .thenReturn(landingZoneManager);

    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    Mockito.when(resourceReader.listResources()).thenReturn(deployedResources);
    var deployedVNetResources =
        List.of(
            new DeployedVNet(VNET_3, subnetHashMap2, REGION),
            new DeployedVNet(VNET_1, subnetHashMap1, REGION));

    Mockito.when(resourceReader.listVNets()).thenReturn(deployedVNetResources);
    Mockito.when(landingZoneManager.reader()).thenReturn(resourceReader);

    LandingZoneResourcesByPurpose result =
        landingZoneService.listResourcesWithPurposes(azureCloudContext);

    assertNotNull(result.generalResources());
    assertNotNull(result.subnetResources());
    assertEquals(2, result.subnetResources().size(), "Two groups of subnet resources expected");
    assertEquals(2, result.generalResources().size(), "Two groups of general resources expected");
    assertTrue(
        result
            .subnetResources()
            .containsKey(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString()));
    assertTrue(
        result
            .subnetResources()
            .containsKey(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET.toString()));
    assertTrue(result.generalResources().containsKey(ResourcePurpose.SHARED_RESOURCE.toString()));
    assertTrue(result.generalResources().containsKey(ResourcePurpose.WLZ_RESOURCE.toString()));
    // Validate number of members in each group
    assertEquals(
        2, result.generalResources().get(ResourcePurpose.SHARED_RESOURCE.toString()).size());
    assertEquals(1, result.generalResources().get(ResourcePurpose.WLZ_RESOURCE.toString()).size());
    assertEquals(
        2,
        result
            .subnetResources()
            .get(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString())
            .size());
    assertEquals(
        1,
        result
            .subnetResources()
            .get(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET.toString())
            .size());
  }

  private void setupAzureCloudContextMock(
      String tenantId, String subscriptionId, String resourceGroupId) {
    when(azureCloudContext.getAzureTenantId()).thenReturn(tenantId);
    when(azureCloudContext.getAzureSubscriptionId()).thenReturn(subscriptionId);
    when(azureCloudContext.getAzureResourceGroupId()).thenReturn(resourceGroupId);
  }

  private long countAzureLandingZoneTemplateRecordsWithAttribute(
      List<LandingZoneDefinition> list,
      String name,
      String description,
      String className,
      String version) {
    return list.stream()
        .filter(
            t ->
                t.name().equals(name)
                    && t.description().equals(description)
                    && t.definition().equals(className)
                    && t.version().equals(version))
        .count();
  }
}
