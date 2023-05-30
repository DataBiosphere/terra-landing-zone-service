package bio.terra.landingzone.stairway.flight.create;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.job.JobMapKeys;
import bio.terra.landingzone.job.LandingZoneJobBuilder;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.landingzone.job.exception.InternalStairwayException;
import bio.terra.landingzone.job.exception.JobNotFoundException;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.library.landingzones.TestArmResourcesFactory;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.resource.step.AggregateLandingZoneResourcesStep;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.exception.FlightNotFoundException;
import bio.terra.stairway.exception.StairwayException;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Tag("integration")
@ActiveProfiles("test")
@PropertySource(value = "classpath:integration_azure_env.properties", ignoreResourceNotFound = true)
@ExtendWith(SpringExtension.class)
@SpringBootTest()
@SpringBootApplication(
    scanBasePackages = {
      "bio.terra.common.logging",
      "bio.terra.common.migrate",
      "bio.terra.common.kubernetes",
      "bio.terra.common.stairway",
      "bio.terra.landingzone"
    },
    exclude = {DataSourceAutoConfiguration.class})
@EnableRetry
@EnableTransactionManagement
public class CreateLandingZoneResourcesFlightIntegrationTest extends BaseIntegrationTest {
  private static final int LANDING_ZONE_RESOURCES_DELETED_AWAIT_TIMEOUT_MINUTES = 2;
  private static final int LANDING_ZONE_RESOURCES_AVAILABLE_AWAIT_TIMEOUT_MINUTES = 2;
  // includes time for rollback too in case of any issues
  private static final int LZ_CREATED_AWAIT_TIMEOUT_MINUTES = 30;
  private static final int LZ_DELETED_AWAIT_TIMEOUT_MINUTES = 20;

  @Mock private BearerToken bearerToken;
  @MockBean private LandingZoneDao landingZoneDao;

  @Autowired LandingZoneService landingZoneService;
  @Autowired LandingZoneJobService azureLandingZoneJobService;

  @Autowired
  @Qualifier("landingZoneStairwayComponent")
  StairwayComponent stairwayComponent;

  UUID jobId;
  UUID landingZoneId;
  ProfileModel profile;
  LandingZoneManager landingZoneManager;

  @BeforeAll
  static void init() {
    Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
  }

  @BeforeEach
  void setup() {

    // we need to use isolated resource group for each test
    resourceGroup =
        TestArmResourcesFactory.createTestResourceGroup(armManagers.azureResourceManager());

    jobId = UUID.randomUUID();
    landingZoneId = UUID.randomUUID();

    profile =
        new ProfileModel()
            .managedResourceGroupId(resourceGroup.name())
            .subscriptionId(UUID.fromString(azureProfile.getSubscriptionId()))
            .tenantId(UUID.fromString(azureProfile.getTenantId()))
            .cloudPlatform(CloudPlatform.AZURE)
            .description("dummyProfile")
            .id(UUID.randomUUID());
    landingZoneManager =
        LandingZoneManager.createLandingZoneManager(
            tokenCredential, azureProfile, resourceGroup.name());
  }

  @AfterEach
  void cleanUpResources() {
    armManagers.azureResourceManager().resourceGroups().deleteByName(resourceGroup.name());
  }

  @Test
  void createResourcesFlightDeploysCromwellResources() {
    String resultPath = "";

    var request =
        LandingZoneRequestFixtures.createCromwellLZRequest(landingZoneId, profile.getId());

    landingZoneService.startLandingZoneResourceCreationJob(
        jobId.toString(), request, profile, landingZoneId, bearerToken, resultPath);

    await()
        .atMost(Duration.ofMinutes(LZ_CREATED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(
            () -> {
              var flightState = retrieveFlightState(jobId.toString());
              assertThat(flightState.getFlightStatus(), not(FlightStatus.RUNNING));
            });
    var flightState = retrieveFlightState(jobId.toString());
    assertThat(flightState.getFlightStatus(), is(FlightStatus.SUCCESS));

    var lzIdString = landingZoneId.toString();

    var resources = landingZoneManager.reader().listSharedResources(lzIdString);
    assertThat(resources, hasSize(AggregateLandingZoneResourcesStep.deployedResourcesKeys.size()));
  }

  @Test
  void createResourcesFlightDeploysProtectedDataResourcesAndDeleteIt() {
    // Step 1 - create lz
    var request =
        LandingZoneRequestFixtures.createProtectedDataLZRequest(landingZoneId, profile.getId());

    landingZoneService.startLandingZoneResourceCreationJob(
        jobId.toString(), request, profile, landingZoneId, bearerToken, "");

    await()
        .atMost(Duration.ofMinutes(LZ_CREATED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertFlightChangeStatusFromRunning(jobId.toString()));
    var flightState = retrieveFlightState(jobId.toString());
    assertThat(flightState.getFlightStatus(), is(FlightStatus.SUCCESS));

    await()
        .atMost(Duration.ofMinutes(LANDING_ZONE_RESOURCES_AVAILABLE_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertLandingZoneSharedResourcesExisted(landingZoneId));

    // Step 2 - delete lz
    var existingLandingZoneRecord =
        LandingZoneRequestFixtures.createLandingZoneRecord(
            landingZoneId, resourceGroup.name(), azureProfile, profile);
    // even with isolated Flight for resource deletion we need to mock dao interaction
    when(landingZoneDao.getLandingZoneRecord(landingZoneId)).thenReturn(existingLandingZoneRecord);

    UUID deleteJobId = UUID.randomUUID();
    startLandingZoneResourceDeletionFlight(deleteJobId.toString(), landingZoneId, bearerToken, "");
    await()
        .atMost(Duration.ofMinutes(LZ_DELETED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertFlightChangeStatusFromRunning(deleteJobId.toString()));
    var deleteFlightState = retrieveFlightState(deleteJobId.toString());
    assertThat(deleteFlightState.getFlightStatus(), is(FlightStatus.SUCCESS));

    await()
        .atMost(Duration.ofMinutes(LANDING_ZONE_RESOURCES_DELETED_AWAIT_TIMEOUT_MINUTES))
        .untilAsserted(() -> assertLandingZoneResourcesDeleted(landingZoneId));
  }

  // just a clone of the method in LandingZoneService
  // but using the stairwayComponent directly means we don't have to fuss around with
  // authentication,
  // which brings in the requirement for a lot more mocking
  private FlightState retrieveFlightState(String jobId) {
    try {
      return stairwayComponent.get().getFlightState(jobId);
    } catch (FlightNotFoundException flightNotFoundException) {
      throw new JobNotFoundException(
          "The flight " + jobId + " was not found", flightNotFoundException);
    } catch (StairwayException | InterruptedException stairwayEx) {
      throw new InternalStairwayException(stairwayEx);
    }
  }

  private void startLandingZoneResourceDeletionFlight(
      String jobId, UUID landingZoneId, BearerToken bearerToken, String resultPath) {
    String jobDescription = "Deleting Azure Landing Zone. Landing Zone ID:%s";
    final LandingZoneJobBuilder jobBuilder =
        azureLandingZoneJobService
            .newJob()
            .jobId(jobId)
            .description(String.format(jobDescription, landingZoneId.toString()))
            // Real deletion flight contains SAM and DB interaction and LZ resources clean up.
            // This flight is only limited to cleaning up the specific LZ resources.
            .flightClass(TestDeleteLandingZoneResourcesFlight.class)
            .operationType(OperationType.DELETE)
            .bearerToken(bearerToken)
            .addParameter(LandingZoneFlightMapKeys.LANDING_ZONE_ID, landingZoneId)
            .addParameter(JobMapKeys.RESULT_PATH.getKeyName(), resultPath);
    jobBuilder.submit();
  }

  private void assertFlightChangeStatusFromRunning(String jobId) {
    var flightState = retrieveFlightState(jobId);
    assertThat(flightState.getFlightStatus(), not(FlightStatus.RUNNING));
  }

  private void assertLandingZoneResourcesDeleted(UUID landingZoneId) {
    var resources = landingZoneManager.reader().listAllResources(landingZoneId.toString());
    assertThat(resources, hasSize(0));
  }

  private void assertLandingZoneSharedResourcesExisted(UUID landingZoneId) {
    var resources = landingZoneManager.reader().listAllResources(landingZoneId.toString());
    assertFalse(resources.isEmpty());
  }
}
