package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.common.model.TargetManagedResourceGroup;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.management.Region;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CreatePostgresqlDbStepTest extends BaseStepTest {
  private static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  private static final String POSTGRESQL_NAME = "testPostgresql";
  private static final String POSTGRESQL_ID = "postgresqlId";

  private static final String postgresqlSku = "psqlSku";
  private static final ServerVersion serverVersion = ServerVersion.ONE_ONE;
  private static final SkuTier skuTier = SkuTier.GENERAL_PURPOSE;
  private static final String backupRetention = "10";
  private static final String storageSize = "100";

  @Mock private PostgreSqlManager mockPostgreSqlManager;
  @Mock private Configurations mockConfigurations;
  @Mock private Configuration.DefinitionStages.Blank mockConfigurationsDefinitionStagesBlank;

  @Mock
  private Configuration.DefinitionStages.WithCreate mockConfigurationDefinitionStagesWithCreate;

  @Mock private Servers mockServers;
  @Mock private Server.DefinitionStages.Blank mockServerDefinitionStagesBlank;

  @Mock
  private Server.DefinitionStages.WithResourceGroup mockServerDefinitionStagesWithResourceGroup;

  @Mock private Server.DefinitionStages.WithCreate mockServerDefinitionStagesWithCreate;
  @Mock private Server mockServer;
  @Mock private Administrators mockAdministrators;

  @Captor private ArgumentCaptor<ServerVersion> versionCaptor;
  @Captor private ArgumentCaptor<Sku> skuCaptor;
  @Captor private ArgumentCaptor<Network> networkCaptor;
  @Captor private ArgumentCaptor<AuthConfig> authConfigCaptor;
  @Captor private ArgumentCaptor<String> availabilityZoneCaptor;
  @Captor private ArgumentCaptor<Backup> backupCaptor;
  @Captor private ArgumentCaptor<CreateMode> createModeCaptor;
  @Captor private ArgumentCaptor<HighAvailability> highAvailabilityCaptor;
  @Captor private ArgumentCaptor<Storage> storageCaptor;
  @Captor private ArgumentCaptor<Map<String, String>> postgresqlTagsCaptor;

  private CreatePostgresqlDbStep createPostgresqlDbStep;
  @Mock private ActiveDirectoryAdministrator.DefinitionStages.Blank mockAdministrator;
  @Mock private ActiveDirectoryAdministrator.DefinitionStages.WithCreate mockAdminWithCreate;

  @BeforeEach
  void setup() {
    createPostgresqlDbStep = new CreatePostgresqlDbStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(createPostgresqlDbStep.getResourceType()))
        .thenReturn(POSTGRESQL_NAME);

    final String adminName = "adminName";
    final String adminPrincipalId = "adminPrincipalId";
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneTagKeys.PGBOUNCER_ENABLED.toString(),
            "true",
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateVnetStep.VNET_ID,
            "vnetId",
            CreatePostgresqlDNSStep.POSTGRESQL_DNS_ID,
            "dnsId",
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(adminName).build(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_PRINCIPAL_ID,
            adminPrincipalId,
            LandingZoneFlightMapKeys.CREATE_LANDING_ZONE_PARAMETERS_RESOLVER,
            mockParametersResolver));
    setupArmManagersForDoStep(
        POSTGRESQL_ID, POSTGRESQL_NAME, mrg.region(), mrg.name(), adminPrincipalId, adminName);

    setupMocksForDefaultValues(serverVersion, postgresqlSku, skuTier, backupRetention, storageSize);

    StepResult stepResult = createPostgresqlDbStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    verifyServerProperties(postgresqlSku, skuTier, backupRetention, storageSize, serverVersion);
    verifyBasicTags(postgresqlTagsCaptor.getValue(), LANDING_ZONE_ID);
    // This assertion cannot go in verifyBasicTags as it only applies to PostgresqlDb
    assertTrue(
        postgresqlTagsCaptor
            .getValue()
            .containsKey(LandingZoneTagKeys.PGBOUNCER_ENABLED.toString()));
    assertThat(
        postgresqlTagsCaptor.getValue().get(LandingZoneTagKeys.PGBOUNCER_ENABLED.toString()),
        equalTo("true"));
    verify(mockServerDefinitionStagesWithCreate, times(1)).create();
    verifyNoMoreInteractions(mockServerDefinitionStagesWithCreate);
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createPostgresqlDbStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var workingMap = new FlightMap();
    workingMap.put(CreatePostgresqlDbStep.POSTGRESQL_ID, POSTGRESQL_ID);
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);
    when(mockPostgreSqlManager.servers()).thenReturn(mockServers);
    when(mockArmManagers.postgreSqlManager()).thenReturn(mockPostgreSqlManager);

    var stepResult = createPostgresqlDbStep.undoStep(mockFlightContext);
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
    verify(mockServers, times(1)).deleteById(POSTGRESQL_ID);
  }

  @Test
  void undoStepSuccessWhenDoStepFailed() throws InterruptedException {
    var workingMap = new FlightMap(); // empty, there is no POSTGRESQL_ID key
    when(mockFlightContext.getWorkingMap()).thenReturn(workingMap);

    var stepResult = createPostgresqlDbStep.undoStep(mockFlightContext);

    verify(mockServers, never()).deleteById(anyString());
    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));
  }

  @Test
  void doStepWhenPostgresProvisioningFailedReturnFailureRetry()
      throws InterruptedException, JsonProcessingException {
    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameProvider.getName(createPostgresqlDbStep.getResourceType()))
        .thenReturn(POSTGRESQL_NAME);

    final String adminName = "adminName";
    final String adminPrincipalId = "adminPrincipalId";
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneTagKeys.PGBOUNCER_ENABLED.toString(),
            "true",
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone()),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            mrg,
            CreateVnetStep.VNET_ID,
            "vnetId",
            CreatePostgresqlDNSStep.POSTGRESQL_DNS_ID,
            "dnsId",
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            LandingZoneResource.builder().resourceName(adminName).build(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_PRINCIPAL_ID,
            adminPrincipalId,
            LandingZoneFlightMapKeys.CREATE_LANDING_ZONE_PARAMETERS_RESOLVER,
            mockParametersResolver));

    setupMocksForDefaultValues(serverVersion, postgresqlSku, skuTier, backupRetention, storageSize);

    var errorJson =
        "{\"code\":\"ResourceOperationFailure\",\"message\":\"The resource operation completed with terminal provisioning state 'Failed'.\",\"details\":[{\"code\":\"InternalServerError\",\"message\":\"An unexpected error occured while processing the request. Tracking ID: '8a04238e-7145-42d6-a71e-8a82d6f9f819'\"}]}";
    var exception = buildManagementException("Provisioning failure", errorJson);
    setupArmManagersForFailedPostgresProvisioning(
        POSTGRESQL_NAME, mrg.region(), mrg.name(), exception);

    var stepResult = createPostgresqlDbStep.doStep(mockFlightContext);

    verify(mockServers, never()).deleteById(anyString());
    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_FAILURE_RETRY));
  }

  private void setupArmManagersForDoStep(
      String postgresqlId,
      String name,
      String region,
      String resourceGroup,
      String adminPrincipalId,
      String adminName) {
    when(mockServer.id()).thenReturn(postgresqlId);
    when(mockServer.region()).thenReturn(Region.US_SOUTH_CENTRAL);
    when(mockServerDefinitionStagesWithCreate.create()).thenReturn(mockServer);
    when(mockServerDefinitionStagesWithCreate.withTags(postgresqlTagsCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withSku(skuCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withVersion(versionCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithResourceGroup.withExistingResourceGroup(resourceGroup))
        .thenReturn(mockServerDefinitionStagesWithCreate);

    when(mockServerDefinitionStagesWithCreate.withNetwork(networkCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withAuthConfig(authConfigCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withBackup(backupCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withCreateMode(createModeCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withHighAvailability(
            highAvailabilityCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withStorage(storageCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);

    when(mockServerDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockServerDefinitionStagesWithResourceGroup);
    when(mockServers.define(name)).thenReturn(mockServerDefinitionStagesBlank);
    when(mockPostgreSqlManager.servers()).thenReturn(mockServers);
    when(mockConfigurationDefinitionStagesWithCreate.withValue(any()))
        .thenReturn(mockConfigurationDefinitionStagesWithCreate);
    when(mockConfigurationDefinitionStagesWithCreate.withSource(any()))
        .thenReturn(mockConfigurationDefinitionStagesWithCreate);
    when(mockConfigurationsDefinitionStagesBlank.withExistingFlexibleServer(resourceGroup, name))
        .thenReturn(mockConfigurationDefinitionStagesWithCreate);
    when(mockConfigurations.define(any())).thenReturn(mockConfigurationsDefinitionStagesBlank);
    when(mockPostgreSqlManager.configurations()).thenReturn(mockConfigurations);
    when(mockArmManagers.postgreSqlManager()).thenReturn(mockPostgreSqlManager);

    when(mockPostgreSqlManager.administrators()).thenReturn(mockAdministrators);
    when(mockAdministrators.define(adminPrincipalId)).thenReturn(mockAdministrator);
    when(mockAdministrator.withExistingFlexibleServer(resourceGroup, name))
        .thenReturn(mockAdminWithCreate);
    when(mockAdminWithCreate.withPrincipalName(adminName)).thenReturn(mockAdminWithCreate);
    when(mockAdminWithCreate.withPrincipalType(PrincipalType.SERVICE_PRINCIPAL))
        .thenReturn(mockAdminWithCreate);
  }

  private void setupArmManagersForFailedPostgresProvisioning(
      String name,
      String region,
      String resourceGroup,
      ManagementException throwExceptionDuringCreation) {
    doThrow(throwExceptionDuringCreation).when(mockServerDefinitionStagesWithCreate).create();
    when(mockServerDefinitionStagesWithCreate.withTags(postgresqlTagsCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withSku(skuCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withVersion(versionCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithResourceGroup.withExistingResourceGroup(resourceGroup))
        .thenReturn(mockServerDefinitionStagesWithCreate);

    when(mockServerDefinitionStagesWithCreate.withNetwork(networkCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withAuthConfig(authConfigCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withBackup(backupCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withCreateMode(createModeCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withHighAvailability(
            highAvailabilityCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withStorage(storageCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);

    when(mockServerDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockServerDefinitionStagesWithResourceGroup);
    when(mockServers.define(name)).thenReturn(mockServerDefinitionStagesBlank);
    when(mockPostgreSqlManager.servers()).thenReturn(mockServers);
    when(mockArmManagers.postgreSqlManager()).thenReturn(mockPostgreSqlManager);
  }

  private void setupMocksForDefaultValues(
      ServerVersion serverVersion,
      String postgresqlSku,
      SkuTier skuTier,
      String backupRetention,
      String storageSize) {
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.POSTGRES_SERVER_VERSION.name()))
        .thenReturn(serverVersion.toString());
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.POSTGRES_SERVER_SKU.name()))
        .thenReturn(postgresqlSku);
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.POSTGRES_SERVER_SKU_TIER.name()))
        .thenReturn(skuTier.toString());
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.POSTGRES_SERVER_BACKUP_RETENTION_DAYS
                .name()))
        .thenReturn(backupRetention);
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.POSTGRES_SERVER_STORAGE_SIZE_GB.name()))
        .thenReturn(storageSize);
    when(mockParametersResolver.getValue(
            LandingZoneDefaultParameters.ParametersNames.ENABLE_PGBOUNCER.name()))
        .thenReturn("true");
  }

  private void verifyServerProperties(
      String sku,
      SkuTier skuTier,
      String backupRetention,
      String storageSize,
      ServerVersion serverVersion) {
    assertThat(versionCaptor.getValue(), equalTo(serverVersion));
    assertNotNull(skuCaptor.getValue());
    assertThat(skuCaptor.getValue().name(), equalTo(sku));
    assertThat(skuCaptor.getValue().tier(), equalTo(skuTier));
    assertThat(
        backupCaptor.getValue().backupRetentionDays(), equalTo(Integer.parseInt(backupRetention)));
    assertThat(storageCaptor.getValue().storageSizeGB(), equalTo(Integer.parseInt(storageSize)));
    assertThat(authConfigCaptor.getValue().passwordAuth(), equalTo(PasswordAuthEnum.DISABLED));
    assertThat(
        authConfigCaptor.getValue().activeDirectoryAuth(),
        equalTo(ActiveDirectoryAuthEnum.ENABLED));
  }

  private ManagementException buildManagementException(String message, String managementErrorJson)
      throws JsonProcessingException {
    var error = new ObjectMapper().readValue(managementErrorJson, ManagementError.class);
    return new ManagementException(message, null, error);
  }
}
