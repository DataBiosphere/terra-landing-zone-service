package bio.terra.landingzone.stairway.flight.create.resource.step.postgres;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.FlightTestUtils;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.create.resource.step.BaseStepTest;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneIdentityStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.landingzone.stairway.flight.create.resource.step.ResourceStepFixture;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ActiveDirectoryAdministrator;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Administrators;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class CreatePostgresqlDbAdminStepTest extends BaseStepTest {

  private CreatePostgresqlDbAdminStep testStep;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private PostgreSqlManager mockPostgreSqlManager;

  @BeforeEach
  void setup() {
    testStep = new CreatePostgresqlDbAdminStep(mockArmManagers, mockResourceNameProvider);
  }

  @Test
  void doStepSuccess() throws InterruptedException {
    setupValidFlightContext();
    var mockAdmin = mock(ActiveDirectoryAdministrator.class);
    when(mockAdmin.objectId()).thenReturn("fakeadminId");
    when(mockArmManagers.postgreSqlManager()).thenReturn(mockPostgreSqlManager);
    when(mockPostgreSqlManager
            .administrators()
            .define(anyString())
            .withExistingFlexibleServer(anyString(), anyString())
            .withPrincipalName(anyString())
            .withPrincipalType(any())
            .create())
        .thenReturn(mockAdmin);

    var result = testStep.doStep(mockFlightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
    assertThat(
        mockFlightContext
            .getWorkingMap()
            .get(CreatePostgresqlDbAdminStep.POSTGRESQL_ADMIN_ID, String.class),
        equalTo("fakeadminId"));
  }

  @Test
  void doStepHandleHttpConflict() throws InterruptedException {
    setupValidFlightContext();

    when(mockArmManagers.postgreSqlManager()).thenReturn(mockPostgreSqlManager);
    var exception = mock(ManagementException.class);
    var mockResponse = mock(HttpResponse.class);
    when(mockResponse.getStatusCode()).thenReturn(HttpStatus.CONFLICT.value());
    when(exception.getResponse()).thenReturn(mockResponse);
    when(mockPostgreSqlManager
            .administrators()
            .define(anyString())
            .withExistingFlexibleServer(anyString(), anyString())
            .withPrincipalName(anyString())
            .withPrincipalType(any())
            .create())
        .thenThrow(exception);

    var result = testStep.doStep(mockFlightContext);

    assertThat(result.getStepStatus(), equalTo(StepStatus.STEP_RESULT_SUCCESS));
  }

  @ParameterizedTest
  @MethodSource("inputParameterProvider")
  void doStepMissingInputParameterThrowsException(Map<String, Object> inputParameters) {
    FlightMap flightMapInputParameters =
        FlightTestUtils.prepareFlightInputParameters(inputParameters);
    when(mockFlightContext.getInputParameters()).thenReturn(flightMapInputParameters);

    assertThrows(MissingRequiredFieldsException.class, () -> testStep.doStep(mockFlightContext));
  }

  @Test
  void undoStepSuccess() throws InterruptedException {
    var resourceId = "resourceId";

    var mockAdmin = mock(Administrators.class);
    when(mockArmManagers.postgreSqlManager()).thenReturn(mockPostgreSqlManager);
    when(mockPostgreSqlManager.administrators()).thenReturn(mockAdmin);
    setupFlightContext(
        mockFlightContext,
        null,
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            ResourceStepFixture.createDefaultMrg(),
            CreatePostgresqlDbAdminStep.POSTGRESQL_ADMIN_ID,
            resourceId));
    var stepResult = testStep.undoStep(mockFlightContext);

    assertThat(stepResult, equalTo(StepResult.getStepResultSuccess()));

    verify(mockAdmin).deleteById(resourceId);
  }

  private void setupValidFlightContext() {
    Map<String, Object> inputParams =
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID,
            LandingZoneFlightMapKeys.LANDING_ZONE_CREATE_PARAMS,
            ResourceStepFixture.createLandingZoneRequestForCromwellLandingZone());
    Map<String, Object> workingMap =
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            ResourceStepFixture.createDefaultMrg(),
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_PRINCIPAL_ID,
            "fake",
            CreatePostgresqlDbStep.POSTGRESQL_NAME,
            "fake",
            CreateLandingZoneIdentityStep.LANDING_ZONE_IDENTITY_RESOURCE_KEY,
            new LandingZoneResource(
                "fake",
                "identity",
                Collections.emptyMap(),
                "fake",
                Optional.of("fakeuami"),
                Optional.empty()));
    setupFlightContext(mockFlightContext, inputParams, workingMap);
  }
}
