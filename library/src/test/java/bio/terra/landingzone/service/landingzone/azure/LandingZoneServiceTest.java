package bio.terra.landingzone.service.landingzone.azure;

import static bio.terra.landingzone.library.landingzones.TestUtils.STUB_BATCH_ACCOUNT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.exception.DuplicateLandingZoneException;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.LandingZoneJobBuilder;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.LandingZoneManagerProvider;
import bio.terra.landingzone.library.configuration.LandingZoneTestingConfiguration;
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
import bio.terra.landingzone.service.bpm.LandingZoneBillingProfileManagerService;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.landingzone.service.iam.SamConstants;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDefinitionNotFound;
import bio.terra.landingzone.service.landingzone.azure.exception.LandingZoneDeleteNotImplemented;
import bio.terra.landingzone.service.landingzone.azure.model.DeletedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.DeployedLandingZone;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneDefinition;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResourcesByPurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.StepsDefinitionFactoryType;
import com.azure.core.management.Region;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class LandingZoneServiceTest {
  private static final String VNET_1 = "vnet_1";
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
  private static final BearerToken bearerToken = new BearerToken("fake-token");
  private static final UUID billingProfileId = UUID.randomUUID();
  private static final OffsetDateTime createdDate = Instant.now().atOffset(ZoneOffset.UTC);

  private LandingZoneService landingZoneService;

  @Mock private LandingZoneManager landingZoneManager;

  @Mock private LandingZoneJobService landingZoneJobService;

  @Captor ArgumentCaptor<String> jobIdCaptor;
  @Captor ArgumentCaptor<Class<?>> classCaptor;
  @Captor ArgumentCaptor<LandingZoneTarget> landingZoneTargetCaptor;

  @Mock private LandingZoneManagerProvider landingZoneManagerProvider;
  @Mock private LandingZoneDao landingZoneDao;
  @Mock private LandingZoneSamService samService;
  @Mock private LandingZoneBillingProfileManagerService bpmService;
  @Mock private LandingZoneTestingConfiguration testingConfiguration;
  @Captor ArgumentCaptor<UUID> captorLandingZoneId;

  @BeforeEach
  void setup() {
    landingZoneService =
        new LandingZoneService(
            landingZoneJobService,
            landingZoneManagerProvider,
            landingZoneDao,
            samService,
            bpmService,
            testingConfiguration);
  }

  @Test
  void getAsyncJobResult_success() {
    String jobId = "newJobId";
    landingZoneService.getAsyncJobResult(bearerToken, jobId);

    verify(landingZoneJobService, times(1))
        .retrieveAsyncJobResult(jobIdCaptor.capture(), classCaptor.capture());
    assertEquals(jobId, jobIdCaptor.getValue());
    assertEquals(DeployedLandingZone.class, classCaptor.getValue());
  }

  @Test
  void getAsyncDeletionJobResult_success() {
    String jobId = "newJobId";
    UUID landingZoneId = UUID.randomUUID();
    landingZoneService.getAsyncDeletionJobResult(bearerToken, landingZoneId, jobId);

    verify(landingZoneJobService, times(1))
        .retrieveAsyncJobResult(jobIdCaptor.capture(), classCaptor.capture());
    verify(landingZoneJobService, times(1))
        .verifyUserAccessForDeleteJobResult(bearerToken, landingZoneId, jobId);

    assertEquals(jobId, jobIdCaptor.getValue());
    assertEquals(DeletedLandingZone.class, classCaptor.getValue());
  }

  @Test
  void getAsyncDeletionJobResult() {
    String jobId = "newJobId";
    UUID landingZoneId = UUID.randomUUID();
    landingZoneService.getAsyncDeletionJobResult(bearerToken, landingZoneId, jobId);

    verify(landingZoneJobService, times(1))
        .retrieveAsyncJobResult(jobIdCaptor.capture(), classCaptor.capture());
    verify(landingZoneJobService, times(1))
        .verifyUserAccessForDeleteJobResult(bearerToken, landingZoneId, jobId);

    assertEquals(jobId, jobIdCaptor.getValue());
    assertEquals(DeletedLandingZone.class, classCaptor.getValue());
  }

  @Test
  void startLandingZoneCreationJob_NoLandingZoneId_JobIsSubmitted() {
    LandingZoneJobBuilder mockJobBuilder = createMockJobBuilder(OperationType.CREATE);
    when(mockJobBuilder.landingZoneRequest(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.addParameter(any(), any())).thenReturn(mockJobBuilder);
    when(landingZoneJobService.newJob()).thenReturn(mockJobBuilder);

    LandingZoneRequest landingZoneRequest =
        LandingZoneRequest.builder()
            .definition(
                StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue())
            .version("V1")
            .parameters(null)
            .billingProfileId(billingProfileId)
            .build();
    landingZoneService.startLandingZoneCreationJob(
        bearerToken, "newJobId", landingZoneRequest, "create-result");

    verify(mockJobBuilder, times(1))
        .addParameter(eq(LandingZoneFlightMapKeys.LANDING_ZONE_ID), captorLandingZoneId.capture());
    assertNotEquals(landingZoneId, captorLandingZoneId.getValue());
    verify(landingZoneJobService, times(1)).newJob();
    verify(mockJobBuilder, times(1)).submit();
  }

  @Test
  void startLandingZoneCreationJob_WithLandingZoneId_JobIsSubmitted() {
    LandingZoneJobBuilder mockJobBuilder = createMockJobBuilder(OperationType.CREATE);
    when(mockJobBuilder.landingZoneRequest(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.addParameter(any(), any())).thenReturn(mockJobBuilder);
    when(landingZoneJobService.newJob()).thenReturn(mockJobBuilder);

    LandingZoneRequest landingZoneRequest =
        LandingZoneRequest.builder()
            .definition(
                StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue())
            .version("V1")
            .parameters(null)
            .billingProfileId(billingProfileId)
            .landingZoneId(landingZoneId)
            .build();
    landingZoneService.startLandingZoneCreationJob(
        bearerToken, "newJobId", landingZoneRequest, "create-result");

    verify(mockJobBuilder, times(1))
        .addParameter(eq(LandingZoneFlightMapKeys.LANDING_ZONE_ID), eq(landingZoneId));
    verify(landingZoneJobService, times(1)).newJob();
    verify(mockJobBuilder, times(1)).submit();
  }

  @Test
  void startLandingZoneCreationJob_WithLandingZoneId_ErrorWithDuplicateId() {
    when(landingZoneDao.getLandingZoneIfExists(eq(landingZoneId)))
        .thenReturn(Optional.of(createLandingZoneRecord()));

    LandingZoneRequest landingZoneRequest =
        LandingZoneRequest.builder()
            .definition(
                StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue())
            .version("V1")
            .parameters(null)
            .billingProfileId(billingProfileId)
            .landingZoneId(landingZoneId)
            .build();

    Assertions.assertThrows(
        DuplicateLandingZoneException.class,
        () ->
            landingZoneService.startLandingZoneCreationJob(
                bearerToken, "newJobId", landingZoneRequest, "create-result"));
  }

  @Test
  void startLandingZoneCreationJob_ThrowsErrorWhenDefinitionDoesntExist() {
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
              .billingProfileId(billingProfileId)
              .build();
      Assertions.assertThrows(
          LandingZoneDefinitionNotFound.class,
          () ->
              landingZoneService.startLandingZoneCreationJob(
                  bearerToken, "jobId", landingZoneRequest, "create-result"));
    }
  }

  @Test
  void startLandingZoneCreationJob_ThrowsErrorWhenDefinitionDoesntExistForStairwayPath() {
    LandingZoneRequest landingZoneRequest =
        LandingZoneRequest.builder()
            .definition("NotExistingDefinition")
            .version(DefinitionVersion.V5.toString())
            .parameters(null)
            .billingProfileId(billingProfileId)
            .build();
    Assertions.assertThrows(
        LandingZoneDefinitionNotFound.class,
        () ->
            landingZoneService.startLandingZoneCreationJob(
                bearerToken, "jobId", landingZoneRequest, "create-result"));
  }

  @Test
  void startLandingZoneCreationJob_ThrowsErrorWhenAttachingInvalidConfiguration() {
    when(testingConfiguration.isAllowAttach()).thenReturn(false);

    LandingZoneRequest landingZoneRequest =
        LandingZoneRequest.builder()
            .definition(
                StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue())
            .version(DefinitionVersion.V1.toString())
            .parameters(Map.of(LandingZoneFlightMapKeys.ATTACH, "true"))
            .billingProfileId(billingProfileId)
            .landingZoneId(landingZoneId)
            .build();

    Assertions.assertThrows(
        BadRequestException.class,
        () ->
            landingZoneService.startLandingZoneCreationJob(
                bearerToken, "newJobId", landingZoneRequest, "create-result"));
  }

  @Test
  void startLandingZoneDeletionJob_JobIsSubmitted() {
    var landingZoneId = UUID.randomUUID();
    String resultPath = "delete-result";

    LandingZoneJobBuilder mockJobBuilder = createMockJobBuilder(OperationType.DELETE);
    when(mockJobBuilder.addParameter(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId))
        .thenReturn(mockJobBuilder);
    when(mockJobBuilder.addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath))
        .thenReturn(mockJobBuilder);
    when(landingZoneJobService.newJob()).thenReturn(mockJobBuilder);

    landingZoneService.startLandingZoneDeletionJob(
        bearerToken, "newJobId", landingZoneId, resultPath);

    verify(landingZoneJobService, times(1)).newJob();
    verify(mockJobBuilder, times(1)).submit();
  }

  @Test
  void listResourcesByPurpose_Success() {
    LandingZoneRecord landingZoneRecord = createLandingZoneRecord();
    // Setup mocks
    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(landingZoneRecord);
    when(landingZoneManagerProvider.createLandingZoneManager(landingZoneTargetCaptor.capture()))
        .thenReturn(landingZoneManager);
    var purposeTags =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.SHARED_RESOURCE.toString(),
            LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
            landingZoneId.toString());
    var deployedResources =
        List.of(
            new DeployedResource(VNET_1, VIRTUAL_NETWORK, purposeTags, REGION),
            new DeployedResource(VNET_SUBNET_1, SUBNET, purposeTags, REGION));
    ResourcesReader resourceReader = mock(ResourcesReader.class);
    when(resourceReader.listResourcesByPurpose(any(), any())).thenReturn(deployedResources);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    // Test
    List<LandingZoneResource> resources =
        landingZoneService.listResourcesByPurpose(
            bearerToken, landingZoneId, ResourcePurpose.SHARED_RESOURCE);

    assertNotNull(resources);
    assertEquals(2, resources.size());
  }

  @Test
  void listResourcesBySubnetPurpose_Success() {
    var subnetList1 =
        List.of(
            new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_1, VNET_1, REGION),
            new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_2, VNET_3, REGION));
    var subnetList2 =
        List.of(new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_3, VNET_3, REGION));

    // Setup Mocks
    LandingZoneRecord landingZoneRecord = createLandingZoneRecord();
    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(landingZoneRecord);
    when(landingZoneManagerProvider.createLandingZoneManager(landingZoneTargetCaptor.capture()))
        .thenReturn(landingZoneManager);

    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(resourceReader.listSubnetsBySubnetPurpose(
            landingZoneId.toString(), SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET))
        .thenReturn(subnetList1);
    when(resourceReader.listSubnetsBySubnetPurpose(
            landingZoneId.toString(), SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET))
        .thenReturn(subnetList2);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    // Test
    List<LandingZoneResource> resources_storage_subnet =
        landingZoneService.listResourcesByPurpose(
            bearerToken, landingZoneId, SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET);

    assertNotNull(resources_storage_subnet);
    assertEquals(1, resources_storage_subnet.size());
    assertTrue(
        resources_storage_subnet.containsAll(
            subnetList2.stream().map(s -> toLandingZoneResource(s)).toList()));

    List<LandingZoneResource> resources_compute_subnet =
        landingZoneService.listResourcesByPurpose(
            bearerToken, landingZoneId, SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);

    assertNotNull(resources_compute_subnet);
    assertEquals(2, resources_compute_subnet.size());
    assertTrue(
        resources_compute_subnet.containsAll(
            subnetList1.stream().map(s -> toLandingZoneResource(s)).toList()));
  }

  @Test
  void listLandingZoneDefinitions_Success() {
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
      List<LandingZoneDefinition> templates =
          landingZoneService.listLandingZoneDefinitions(bearerToken);

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
  void deleteAzureLandingZone_ThrowsException() {
    Assertions.assertThrows(
        LandingZoneDeleteNotImplemented.class,
        () -> landingZoneService.deleteLandingZone(bearerToken, landingZoneId),
        "Delete operation is not supported");
  }

  @Test
  void listResourcesWithPurposes_LandingZoneManagerIsCreatedWithCorrectTargetParameters() {
    LandingZoneRecord landingZoneRecord = createLandingZoneRecord();

    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(landingZoneRecord);
    when(landingZoneManagerProvider.createLandingZoneManager(landingZoneTargetCaptor.capture()))
        .thenReturn(landingZoneManager);
    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    // Test
    landingZoneService.listResourcesWithPurposes(bearerToken, landingZoneId);
    assertThat(
        landingZoneTargetCaptor.getValue().azureTenantId(), equalTo(landingZoneRecord.tenantId()));
    assertThat(
        landingZoneTargetCaptor.getValue().azureResourceGroupId(),
        equalTo(landingZoneRecord.resourceGroupId()));
    assertThat(
        landingZoneTargetCaptor.getValue().azureSubscriptionId(),
        equalTo(landingZoneRecord.subscriptionId()));
  }

  @Test
  void listGeneralResourcesWithPurposes_Success() {
    var deployedResources = setupDeployedResources();
    // Setup mocks
    LandingZoneRecord landingZoneRecord = createLandingZoneRecord();

    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(landingZoneRecord);
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);
    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(resourceReader.listResourcesWithPurpose(anyString())).thenReturn(deployedResources);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    // Test
    var result = landingZoneService.listResourcesWithPurposes(bearerToken, landingZoneId);
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
    assertEquals(1, resourcesGrouped.get(ResourcePurpose.SHARED_RESOURCE).size());
    assertEquals(1, resourcesGrouped.get(ResourcePurpose.WLZ_RESOURCE).size());
  }

  private LandingZoneRecord createLandingZoneRecord() {
    return new LandingZoneRecord(
        landingZoneId,
        "resourceGroupId",
        "definition",
        "version",
        "subscriptionId",
        "tenantId",
        billingProfileId,
        null,
        createdDate,
        Optional.of("displayName"),
        Optional.of("description"),
        Collections.emptyMap());
  }

  @Test
  void listResourcesWithPurposes_GeneralAndSubnetResourcesReturned_Success() {
    var deployedResources = setupDeployedResources();
    var subnetList1 =
        List.of(
            new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_1, VNET_1, REGION),
            new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_2, VNET_3, REGION));
    var subnetList2 =
        List.of(new DeployedSubnet(UUID.randomUUID().toString(), VNET_SUBNET_3, VNET_3, REGION));

    // Setup Mocks
    LandingZoneRecord landingZoneRecord = createLandingZoneRecord();
    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(landingZoneRecord);
    when(landingZoneManagerProvider.createLandingZoneManager(any(LandingZoneTarget.class)))
        .thenReturn(landingZoneManager);

    ResourcesReader resourceReader = Mockito.mock(ResourcesReader.class);
    when(resourceReader.listResourcesWithPurpose(landingZoneId.toString()))
        .thenReturn(deployedResources);
    when(resourceReader.listSubnetsBySubnetPurpose(anyString(), any(SubnetResourcePurpose.class)))
        .thenReturn(List.of());
    when(resourceReader.listSubnetsBySubnetPurpose(
            eq(landingZoneId.toString()), eq(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET)))
        .thenReturn(subnetList1);
    when(resourceReader.listSubnetsBySubnetPurpose(
            eq(landingZoneId.toString()), eq(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET)))
        .thenReturn(subnetList2);
    when(landingZoneManager.reader()).thenReturn(resourceReader);

    // Test and validate
    LandingZoneResourcesByPurpose result =
        landingZoneService.listResourcesWithPurposes(bearerToken, landingZoneId);

    assertNotNull(result.deployedResources());
    assertEquals(4, result.deployedResources().size(), "Four groups of resources expected");
    assertTrue(
        result.deployedResources().containsKey(SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET));
    assertTrue(
        result.deployedResources().containsKey(SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET));
    assertTrue(result.deployedResources().containsKey(ResourcePurpose.SHARED_RESOURCE));
    assertTrue(result.deployedResources().containsKey(ResourcePurpose.WLZ_RESOURCE));
    // Validate number of members in each group
    assertEquals(1, result.deployedResources().get(ResourcePurpose.SHARED_RESOURCE).size());
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
  void getLandingZone_byLandingZoneId_Success() {
    var deployedResources = setupDeployedResources();
    // Setup mocks
    final var tenantId = UUID.randomUUID();
    final var subscriptionId = UUID.randomUUID();
    final var resourceGroup = "mrg";
    final var definition = "definition";
    final var version = "version";
    LandingZoneRecord landingZoneRecord =
        new LandingZoneRecord(
            landingZoneId,
            resourceGroup,
            definition,
            version,
            subscriptionId.toString(),
            tenantId.toString(),
            billingProfileId,
            null,
            createdDate,
            null,
            null,
            Collections.emptyMap());
    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(landingZoneRecord);

    // Test
    var result = landingZoneService.getLandingZone(bearerToken, landingZoneId);
    // Validate record
    assertNotNull(result);
    assertEquals(landingZoneId, result.landingZoneId());
    assertEquals(billingProfileId, result.billingProfileId());
    assertEquals(definition, result.definition());
    assertEquals(version, result.version());
  }

  @Test
  void getLandingZone_DatabaseException_ThrowsException() {
    // Setup mocks
    doThrow(new DataRetrievalFailureException("..."))
        .when(landingZoneDao)
        .getLandingZoneRecord(landingZoneId);
    // Test
    Assertions.assertThrows(
        InternalServerErrorException.class,
        () -> landingZoneService.getLandingZone(bearerToken, landingZoneId));
  }

  @Test
  void getLandingZonesByBillingProfile_Success() throws InterruptedException {
    var deployedResources = setupDeployedResources();
    // Setup mocks
    final var tenantId = UUID.randomUUID();
    final var subscriptionId = UUID.randomUUID();
    final var resourceGroup = "mrg";
    final var definition = "definition";
    final var version = "version";
    LandingZoneRecord landingZoneRecord =
        new LandingZoneRecord(
            landingZoneId,
            resourceGroup,
            definition,
            version,
            subscriptionId.toString(),
            tenantId.toString(),
            billingProfileId,
            null,
            createdDate,
            null,
            null,
            Collections.emptyMap());
    when(landingZoneDao.getLandingZoneByBillingProfileId(billingProfileId))
        .thenReturn(landingZoneRecord);
    when(samService.isAuthorized(
            any(),
            eq(SamConstants.SamResourceType.LANDING_ZONE),
            eq(landingZoneId.toString()),
            anyString()))
        .thenReturn(true);

    // Test
    var result = landingZoneService.getLandingZonesByBillingProfile(bearerToken, billingProfileId);
    // Validate record
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(landingZoneId, result.get(0).landingZoneId());
    assertEquals(billingProfileId, result.get(0).billingProfileId());
    assertEquals(definition, result.get(0).definition());
    assertEquals(version, result.get(0).version());
    assertEquals(createdDate, result.get(0).createdDate());
  }

  @Test
  void getLandingZonesByBillingProfile_UserIsNotAuthorized_NoRecords() throws InterruptedException {
    var deployedResources = setupDeployedResources();
    // Setup mocks
    final var tenantId = UUID.randomUUID();
    final var subscriptionId = UUID.randomUUID();
    final var resourceGroup = "mrg";
    final var definition = "definition";
    final var version = "version";
    LandingZoneRecord landingZoneRecord =
        new LandingZoneRecord(
            landingZoneId,
            resourceGroup,
            definition,
            version,
            subscriptionId.toString(),
            tenantId.toString(),
            billingProfileId,
            null,
            createdDate,
            null,
            null,
            Collections.emptyMap());
    when(landingZoneDao.getLandingZoneByBillingProfileId(billingProfileId))
        .thenReturn(landingZoneRecord);
    when(samService.isAuthorized(
            any(),
            eq(SamConstants.SamResourceType.LANDING_ZONE),
            eq(landingZoneId.toString()),
            anyString()))
        .thenReturn(false);

    // Test
    var result = landingZoneService.getLandingZonesByBillingProfile(bearerToken, billingProfileId);
    // Validate there are no records in result.
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  void getLandingZoneByBillingProfileId_DatabaseException_ThrowsException() {
    UUID billingProfileId = UUID.randomUUID();
    // Setup mocks
    doThrow(new DataRetrievalFailureException("..."))
        .when(landingZoneDao)
        .getLandingZoneByBillingProfileId(billingProfileId);
    // Test
    Assertions.assertThrows(
        InternalServerErrorException.class,
        () -> landingZoneService.getLandingZonesByBillingProfile(bearerToken, billingProfileId));
  }

  @Test
  void listLandingZones_OneRecord_Success() throws InterruptedException {
    var deployedResources = setupDeployedResources();
    // Setup mocks
    final var tenantId = UUID.randomUUID();
    final var subscriptionId = UUID.randomUUID();
    final var resourceGroup = "mrg";
    final var definition = "definition";
    final var version = "version";
    LandingZoneRecord landingZoneRecord =
        new LandingZoneRecord(
            landingZoneId,
            resourceGroup,
            definition,
            version,
            subscriptionId.toString(),
            tenantId.toString(),
            billingProfileId,
            null,
            createdDate,
            null,
            null,
            Collections.emptyMap());
    when(samService.listLandingZoneResourceIds(bearerToken)).thenReturn(List.of(landingZoneId));
    when(landingZoneDao.getLandingZoneMatchingIdList(List.of(landingZoneId)))
        .thenReturn(List.of(landingZoneRecord));

    // Test
    var result = landingZoneService.listLandingZones(bearerToken);
    // Validate there are no records in result.
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(landingZoneId, result.get(0).landingZoneId());
    assertEquals(billingProfileId, result.get(0).billingProfileId());
    assertEquals(definition, result.get(0).definition());
    assertEquals(version, result.get(0).version());
    assertEquals(createdDate, result.get(0).createdDate());
  }

  @Test
  void listLandingZones_NoRecords_Success() throws InterruptedException {
    var deployedResources = setupDeployedResources();
    // Setup mocks
    when(samService.listLandingZoneResourceIds(bearerToken)).thenReturn(List.of());
    // Test
    var result = landingZoneService.listLandingZones(bearerToken);
    // Validate there are no records in result.
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  void listLandingZones_DatabaseException_ThrowsException() throws InterruptedException {
    UUID dummylandingZoneId = UUID.randomUUID();
    // Setup mocks
    when(samService.listLandingZoneResourceIds(bearerToken))
        .thenReturn(List.of(dummylandingZoneId));
    doThrow(new DataRetrievalFailureException("..."))
        .when(landingZoneDao)
        .getLandingZoneMatchingIdList(List.of(dummylandingZoneId));
    // Test
    Assertions.assertThrows(
        InternalServerErrorException.class, () -> landingZoneService.listLandingZones(bearerToken));
  }

  @Test
  void getLandingZone_UserIsNotAuthorized_ThrowsException() throws InterruptedException {
    // Setup mocks
    doThrow(new ForbiddenException("User has no write access"))
        .when(samService)
        .checkAuthz(
            any(),
            eq(SamConstants.SamResourceType.LANDING_ZONE),
            eq(landingZoneId.toString()),
            eq(SamConstants.SamLandingZoneAction.LIST_RESOURCES));

    // Test
    Assertions.assertThrows(
        ForbiddenException.class,
        () -> landingZoneService.getLandingZone(bearerToken, landingZoneId));
  }

  @Test
  void getResourceQuota_userIsAuthorizedAndLZManagerAndSamIsCalled() throws InterruptedException {

    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(createLandingZoneRecord());
    when(landingZoneManagerProvider.createLandingZoneManager(any())).thenReturn(landingZoneManager);

    landingZoneService.getResourceQuota(bearerToken, landingZoneId, STUB_BATCH_ACCOUNT_ID);

    verify(samService, times(1))
        .checkAuthz(
            eq(bearerToken),
            eq(SamConstants.SamResourceType.LANDING_ZONE),
            eq(landingZoneId.toString()),
            eq(SamConstants.SamLandingZoneAction.LIST_RESOURCES));

    verify(landingZoneManager, times(1))
        .resourceQuota(landingZoneId.toString(), STUB_BATCH_ACCOUNT_ID);
  }

  @Test
  void getLandingZoneRegion_returnsCorrectRegion() {
    final Region expectedRegion = Region.ASIA_EAST;

    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(createLandingZoneRecord());
    when(landingZoneManagerProvider.createLandingZoneManager(any())).thenReturn(landingZoneManager);
    when(landingZoneManager.getLandingZoneRegion()).thenReturn(expectedRegion);

    var actualRegionName = landingZoneService.getLandingZoneRegion(bearerToken, landingZoneId);
    assertEquals(expectedRegion.name(), actualRegionName);
  }

  private List<DeployedResource> setupDeployedResources() {
    String landingZoneId = UUID.randomUUID().toString();
    var purposeTagSet1 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
            landingZoneId,
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.SHARED_RESOURCE.toString());
    var purposeTagSet2 =
        Map.of(
            LandingZoneTagKeys.LANDING_ZONE_ID.toString(),
            landingZoneId,
            LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(),
            ResourcePurpose.WLZ_RESOURCE.toString());
    return List.of(
        new DeployedResource(STORAGE_ACCOUNT_1, STORAGE_ACCOUNT, purposeTagSet1, REGION),
        new DeployedResource(STORAGE_ACCOUNT_2, STORAGE_ACCOUNT, purposeTagSet2, REGION));
  }

  private LandingZoneJobBuilder createMockJobBuilder(OperationType operationType) {
    LandingZoneJobBuilder mockJobBuilder = mock(LandingZoneJobBuilder.class);
    when(mockJobBuilder.jobId(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.description(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.flightClass(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.bearerToken(any())).thenReturn(mockJobBuilder);
    when(mockJobBuilder.operationType(operationType)).thenReturn(mockJobBuilder);
    return mockJobBuilder;
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

  private LandingZoneResource toLandingZoneResource(DeployedSubnet subnet) {
    return LandingZoneResource.builder()
        .resourceId(subnet.id())
        .resourceType(subnet.getClass().getSimpleName())
        .resourceName(subnet.name())
        .resourceParentId(subnet.vNetId())
        .region(subnet.vNetRegion())
        .build();
  }
}
