package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.PublicNetworkAccessEnum;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.ServerPropertiesForCreate;
import com.azure.resourcemanager.postgresql.models.ServerPropertiesForDefaultCreate;
import com.azure.resourcemanager.postgresql.models.ServerVersion;
import com.azure.resourcemanager.postgresql.models.Servers;
import com.azure.resourcemanager.postgresql.models.Sku;
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

  @Mock private PostgreSqlManager mockPostgreSqlManager;
  @Mock private Servers mockServers;
  @Mock private Server.DefinitionStages.Blank mockServerDefinitionStagesBlank;

  @Mock
  private Server.DefinitionStages.WithResourceGroup mockServerDefinitionStagesWithResourceGroup;

  @Mock private Server.DefinitionStages.WithProperties mockServerDefinitionStagesWithProperties;
  @Mock private Server.DefinitionStages.WithCreate mockServerDefinitionStagesWithCreate;
  @Mock private Server mockServer;

  @Captor private ArgumentCaptor<ServerPropertiesForCreate> serverPropertiesForCreateCaptor;
  @Captor private ArgumentCaptor<Sku> skuCaptor;
  @Captor private ArgumentCaptor<Map<String, String>> postgresqlTagsCaptor;

  private CreatePostgresqlDbStep createPostgresqlDbStep;

  @BeforeEach
  void setup() {
    createPostgresqlDbStep =
        new CreatePostgresqlDbStep(
            mockArmManagers, mockParametersResolver, mockResourceNameGenerator);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    var postgresAdminName = "dbAdmin";
    var postgresPassword = "password";
    var postgresqlSku = "psqlSku";

    TargetManagedResourceGroup mrg = ResourceStepFixture.createDefaultMrg();
    when(mockResourceNameGenerator.nextName(
            ResourceNameGenerator.MAX_POSTGRESQL_SERVER_NAME_LENGTH))
        .thenReturn(POSTGRESQL_NAME);

    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of(GetManagedResourceGroupInfo.TARGET_MRG_KEY, mrg));
    setupArmManagersForDoStep(POSTGRESQL_ID, POSTGRESQL_NAME, mrg.region(), mrg.name());

    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_ADMIN.name()))
        .thenReturn(postgresAdminName);
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_PASSWORD.name()))
        .thenReturn(postgresPassword);
    when(mockParametersResolver.getValue(
            CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_SKU.name()))
        .thenReturn(postgresqlSku);

    StepResult stepResult = createPostgresqlDbStep.doStep(mockFlightContext);

    assertThat(stepResult.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));

    verifyServerProperties(postgresAdminName, postgresPassword, postgresqlSku);
    verifyBasicTags(postgresqlTagsCaptor.getValue(), LANDING_ZONE_ID);
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

  private void setupArmManagersForDoStep(
      String postgresqlId, String name, String region, String resourceGroup) {
    when(mockServer.id()).thenReturn(postgresqlId);
    when(mockServerDefinitionStagesWithCreate.create()).thenReturn(mockServer);
    when(mockServerDefinitionStagesWithCreate.withTags(postgresqlTagsCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithCreate.withSku(skuCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithProperties.withProperties(
            serverPropertiesForCreateCaptor.capture()))
        .thenReturn(mockServerDefinitionStagesWithCreate);
    when(mockServerDefinitionStagesWithResourceGroup.withExistingResourceGroup(resourceGroup))
        .thenReturn(mockServerDefinitionStagesWithProperties);
    when(mockServerDefinitionStagesBlank.withRegion(region))
        .thenReturn(mockServerDefinitionStagesWithResourceGroup);
    when(mockServers.define(name)).thenReturn(mockServerDefinitionStagesBlank);
    when(mockPostgreSqlManager.servers()).thenReturn(mockServers);
    when(mockArmManagers.postgreSqlManager()).thenReturn(mockPostgreSqlManager);
  }

  private void verifyServerProperties(String adminName, String adminPassword, String sku) {
    // verifyProperties
    assertNotNull(serverPropertiesForCreateCaptor.getValue());
    assertThat(
        serverPropertiesForCreateCaptor.getValue().getClass(),
        equalTo(ServerPropertiesForDefaultCreate.class));
    ServerPropertiesForDefaultCreate properties =
        (ServerPropertiesForDefaultCreate) serverPropertiesForCreateCaptor.getValue();
    assertThat(properties.administratorLogin(), equalTo(adminName));
    assertThat(properties.administratorLoginPassword(), equalTo(adminPassword));
    assertThat(properties.version(), equalTo(ServerVersion.ONE_ONE));
    assertThat(properties.publicNetworkAccess(), equalTo(PublicNetworkAccessEnum.DISABLED));

    // verify sku
    assertNotNull(skuCaptor.getValue());
    assertThat(skuCaptor.getValue().name(), equalTo(sku));
  }
}
