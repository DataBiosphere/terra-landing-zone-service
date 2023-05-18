package bio.terra.landingzone.stairway.flight.create;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import bio.terra.common.iam.BearerToken;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.landingzone.job.exception.InternalStairwayException;
import bio.terra.landingzone.job.exception.JobNotFoundException;
import bio.terra.landingzone.library.landingzones.LandingZoneTestFixture;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.flight.StepsDefinitionFactoryType;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.exception.FlightNotFoundException;
import bio.terra.stairway.exception.StairwayException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Tag("integration")
@ExtendWith(SpringExtension.class)
@SpringBootTest()

@SpringBootApplication(
    scanBasePackages = {
        "bio.terra.common.logging",
        "bio.terra.common.migrate",
        "bio.terra.common.kubernetes",
        "bio.terra.landingzone"
    },
    exclude = {DataSourceAutoConfiguration.class}
    )
@EnableRetry
@EnableTransactionManagement
public class CreateLandingZoneResourcesFlightIntegrationTest extends LandingZoneTestFixture {


  @Mock private BearerToken bearerToken;

  @Autowired LandingZoneService landingZoneService;

  @Autowired
  @Qualifier("landingZoneStairwayComponent")
  StairwayComponent stairwayComponent;

  UUID jobId;
  UUID landingZoneId;
  ProfileModel profile;
  LandingZoneRequest request;

  @BeforeEach
  void setup() {
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
    request =
        new LandingZoneRequest(
            StepsDefinitionFactoryType.PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME.getValue(),
            "v1",
            Map.of(),
            profile.getId(),
            Optional.of(landingZoneId),
            true);
  }

  @Test
  void test() {
    String resultPath = "";
    landingZoneService.startLandingZoneResourceCreationJob(
        jobId.toString(), request, profile, landingZoneId, bearerToken, resultPath);

    await()
        .atMost(Duration.ofMinutes(20))
        .untilAsserted(
            () -> {
              var flightState = retrieveFlightState(jobId.toString());
              assertThat(flightState.getFlightStatus(), not(FlightStatus.RUNNING));
            });
    var flightState = retrieveFlightState(jobId.toString());
    assertThat(flightState.getFlightStatus(), oneOf(FlightStatus.READY, FlightStatus.SUCCESS));
  }


  // just a clone of the method in LandingZoneService
  // but using the stairwayComponent directly means we don't have to fuss around with authentication,
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

}