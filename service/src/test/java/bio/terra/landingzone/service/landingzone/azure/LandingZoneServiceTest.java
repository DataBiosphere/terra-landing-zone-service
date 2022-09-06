package bio.terra.landingzone.service.landingzone.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZone;
import bio.terra.landingzone.job.LandingZoneJobBuilder;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import bio.terra.landingzone.library.landingzones.definition.factories.LandingZoneDefinitionFactory;
import bio.terra.landingzone.library.landingzones.deployment.DeployedResource;
import bio.terra.landingzone.library.landingzones.deployment.DeployedSubnet;
import bio.terra.landingzone.library.landingzones.deployment.LandingZonePurpose;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.library.landingzones.management.ResourcesReader;
import bio.terra.landingzone.model.LandingZoneTarget;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResourcesByPurpose;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private static final UUID landingZoneId = UUID.randomUUID();
  private LandingZoneService landingZoneService;

  @Mock private LandingZoneManager landingZoneManager;

  @Mock private LandingZoneJobService landingZoneJobService;

  @Captor ArgumentCaptor<String> jobIdCaptor;
  @Captor ArgumentCaptor<Class<?>> classCaptor;

  @Mock private LandingZoneTarget landingZoneTarget;

  @Mock private LandingZoneManagerProvider landingZoneManagerProvider;
  @Mock private LandingZoneDao landingZoneDao;

  @BeforeEach
  public void setup() {
    landingZoneService =
        new LandingZoneService(landingZoneJobService, landingZoneManagerProvider, landingZoneDao);
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
              .azureCloudContext(landingZoneTarget)
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
              .azureCloudContext(landingZoneTarget)
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

    when(landingZoneManagerProvider.createLandingZoneManager(landingZoneTarget))
        .thenReturn(landingZoneManager);

    ResourcesReader resourceReader = mock(ResourcesReader.class);
    when(resourceReader.listResourcesByPurpose(ArgumentMatchers.any()))
        .thenReturn(deployedResources);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    List<LandingZoneResource> resources =
        landingZoneService.listResourcesByPurpose(
            ResourcePurpose.SHARED_RESOURCE, landingZoneTarget);

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
    var deployedResources = setupDeployedResources();
    // Setup mocks
    LandingZone landingZone =
        new LandingZone(
            landingZoneId,
            "resourceGroupId",
            "definition",
            "version",
            "subscriptionId",
            "tenantId",
            Optional.of("displayName"),
            Optional.of("description"),
            Collections.emptyMap());

    when(landingZoneDao.getLandingZone(landingZoneId)).thenReturn(landingZone);
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);
    landingZoneService =
        new LandingZoneService(landingZoneJobService, landingZoneManagerProvider, landingZoneDao);
    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(resourceReader.listResources()).thenReturn(deployedResources);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    // Test
    var result = landingZoneService.listResourcesWithPurposes(landingZoneId.toString());
    assertNotNull(result);

    Map<LandingZonePurpose, List<LandingZoneResource>> resourcesGrouped =
        result.deployedResources();
    assertNotNull(resourcesGrouped);

    // Validate number of purpose groups returned: two groups expected
    assertEquals(2, resourcesGrouped.size());
    assertTrue(resourcesGrouped.containsKey(ResourcePurpose.SHARED_RESOURCE));
    assertTrue(resourcesGrouped.containsKey(ResourcePurpose.WLZ_RESOURCE));

    assertFalse(resourcesGrouped.containsKey(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET));

    // Validate number of members in each group
    assertEquals(2, resourcesGrouped.get(ResourcePurpose.SHARED_RESOURCE).size());
    assertEquals(1, resourcesGrouped.get(ResourcePurpose.WLZ_RESOURCE).size());
  }

  @Test
  public void listResourcesWithPurposes_GeneralAndSubnetResourcesReturned_Success() {
    var deployedResources = setupDeployedResources();
    var subnetList1 =
        List.of(
            new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_1, VNET_1, REGION),
            new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_2, VNET_3, REGION));
    var subnetList2 =
        List.of(new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_3, VNET_3, REGION));

    // Setup Mocks
    LandingZone landingZone =
        new LandingZone(
            landingZoneId,
            "resourceGroupId",
            "definition",
            "version",
            "subscriptionId",
            "tenantId",
            Optional.of("displayName"),
            Optional.of("description"),
            Collections.emptyMap());
    when(landingZoneDao.getLandingZone(landingZoneId)).thenReturn(landingZone);
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);

    landingZoneService =
        new LandingZoneService(landingZoneJobService, landingZoneManagerProvider, landingZoneDao);

    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(resourceReader.listResources()).thenReturn(deployedResources);
    when(resourceReader.listSubnetsWithSubnetPurpose(any(SubnetResourcePurpose.class)))
        .thenReturn(List.of());
    when(resourceReader.listSubnetsWithSubnetPurpose(
            SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET))
        .thenReturn(subnetList1);
    when(resourceReader.listSubnetsWithSubnetPurpose(
            SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET))
        .thenReturn(subnetList2);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    // Test and validate
    LandingZoneResourcesByPurpose result =
        landingZoneService.listResourcesWithPurposes(landingZoneId.toString());

    Map<LandingZonePurpose, List<LandingZoneResource>> resourcesGrouped =
        result.deployedResources();

    assertNotNull(result.deployedResources());
    assertEquals(4, result.deployedResources().size(), "Four groups of resources expected");
    assertTrue(
        result.deployedResources().containsKey(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET));
    assertTrue(
        result.deployedResources().containsKey(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET));
    assertTrue(result.deployedResources().containsKey(ResourcePurpose.SHARED_RESOURCE));
    assertTrue(result.deployedResources().containsKey(ResourcePurpose.WLZ_RESOURCE));
    // Validate number of members in each group
    assertEquals(2, result.deployedResources().get(ResourcePurpose.SHARED_RESOURCE).size());
    assertEquals(1, result.deployedResources().get(ResourcePurpose.WLZ_RESOURCE).size());
    assertEquals(
        2, result.deployedResources().get(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET).size());
    assertEquals(
        1, result.deployedResources().get(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET).size());
    var subnetResource =
        result.deployedResources().get(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET).get(0);
    assertEquals(subnetList2.get(0).id(), subnetResource.resourceId());
    assertTrue(subnetResource.resourceName().isPresent());
    assertTrue(subnetResource.resourceParentId().isPresent());
    assertEquals(subnetList2.get(0).name(), subnetResource.resourceName().get());
    assertEquals(subnetList2.get(0).vNetId(), subnetResource.resourceParentId().get());
    assertEquals(subnetList2.get(0).vNetRegion(), subnetResource.region());
    assertEquals(
        subnetList2.get(0).getClass().getSimpleName(),
        subnetResource.resourceType(),
        "Resource type doesn't match.");
  }

  @Test
  public void listLandingZoneIds_Success() {
    var deployedResources = setupDeployedResources();
    // Setup mocks
    LandingZone landingZone =
        new LandingZone(
            landingZoneId,
            "resourceGroupId",
            "definition",
            "version",
            "subscriptionId",
            "tenantId",
            null,
            null,
            Collections.emptyMap());
    when(landingZoneDao.getLandingZoneList(anyString(), anyString(), anyString()))
        .thenReturn(List.of(landingZone));
    landingZoneService =
        new LandingZoneService(landingZoneJobService, landingZoneManagerProvider, landingZoneDao);
    setupAzureCloudContextMock("tenantId", "subscriptionId", "resourceGroupId");
    // Test
    var result = landingZoneService.listLandingZoneIds(landingZoneTarget);
    // Validate number of members in each group
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(landingZoneId, UUID.fromString(result.get(0)));
  }

  private void setupAzureCloudContextMock(
      String tenantId, String subscriptionId, String resourceGroupId) {
    when(landingZoneTarget.getAzureTenantId()).thenReturn(tenantId);
    when(landingZoneTarget.getAzureSubscriptionId()).thenReturn(subscriptionId);
    when(landingZoneTarget.getAzureResourceGroupId()).thenReturn(resourceGroupId);
  }

  private List<DeployedResource> setupDeployedResources() {
    var purposeTagSet1 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.SHARED_RESOURCE.toString());
    var purposeTagSet2 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.WLZ_RESOURCE.toString());
    return List.of(
        new DeployedResource(STORAGE_ACCOUNT_1, STORAGE_ACCOUNT, purposeTagSet1, REGION),
        new DeployedResource(VNET_2, VIRTUAL_NETWORK, purposeTagSet2, REGION),
        new DeployedResource(STORAGE_ACCOUNT_2, STORAGE_ACCOUNT, purposeTagSet1, REGION));
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
