package bio.terra.lz.futureservice.app.controller;

import static bio.terra.lz.futureservice.common.utils.MockMvcUtils.USER_REQUEST;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.common.exception.ConflictException;
import bio.terra.common.exception.ForbiddenException;
import bio.terra.lz.futureservice.app.service.LandingZoneAppService;
import bio.terra.lz.futureservice.common.BaseSpringUnitTest;
import bio.terra.lz.futureservice.common.fixture.AzureLandingZoneFixtures;
import bio.terra.lz.futureservice.common.utils.MockMvcUtils;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZone;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDefinition;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDefinitionList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneJobResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiJobReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@AutoConfigureMockMvc
public class LandingZoneApiControllerTest extends BaseSpringUnitTest {
  private static final String AZURE_LANDING_ZONE_PATH = "/api/landingzones/v1/azure";
  private static final String GET_CREATE_AZURE_LANDING_ZONE_RESULT =
      "/api/landingzones/v1/azure/create-result";
  private static final String LIST_AZURE_LANDING_ZONES_DEFINITIONS_PATH =
      "/api/landingzones/definitions/v1/azure";
  private static final String JOB_ID = "newJobId";
  private static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  private static final UUID BILLING_PROFILE_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean LandingZoneAppService mockLandingZoneAppService;

  @Test
  public void createAzureLandingZoneJobRunning() throws Exception {
    ApiCreateLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiCreateLandingZoneSuccessResult(JOB_ID);
    ApiCreateAzureLandingZoneRequestBody requestBody =
        AzureLandingZoneFixtures.buildCreateAzureLandingZoneRequest(JOB_ID, BILLING_PROFILE_ID);

    when(mockLandingZoneAppService.createAzureLandingZone(any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                post(AZURE_LANDING_ZONE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().isAccepted())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id", Matchers.is(JOB_ID)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZone").doesNotExist());
  }

  @Test
  public void createAzureLandingZoneWithoutDefinitionValidationFailed() throws Exception {
    ApiCreateLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiCreateLandingZoneSuccessResult(JOB_ID);
    ApiCreateAzureLandingZoneRequestBody requestBody =
        AzureLandingZoneFixtures.buildCreateAzureLandingZoneRequestWithoutDefinition(JOB_ID);

    when(mockLandingZoneAppService.createAzureLandingZone(any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                post(AZURE_LANDING_ZONE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void createAzureLandingZoneWithoutBillingProfileValidationFailed() throws Exception {
    ApiCreateLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiCreateLandingZoneSuccessResult(JOB_ID);
    ApiCreateAzureLandingZoneRequestBody requestBody =
        AzureLandingZoneFixtures.buildCreateAzureLandingZoneRequestWithoutBillingProfile(JOB_ID);

    when(mockLandingZoneAppService.createAzureLandingZone(any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                post(AZURE_LANDING_ZONE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void getCreateAzureLandingZoneResultJobRunning() throws Exception {
    ApiAzureLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiAzureLandingZoneResult(
            JOB_ID, ApiJobReport.StatusEnum.RUNNING);

    when(mockLandingZoneAppService.getCreateAzureLandingZoneResult(any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(GET_CREATE_AZURE_LANDING_ZONE_RESULT + "/{jobId}", JOB_ID), USER_REQUEST))
        .andExpect(status().isAccepted())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id", Matchers.is(JOB_ID)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZone").doesNotExist());
  }

  @Test
  public void getCreateAzureLandingZoneResultJobSucceeded() throws Exception {
    ApiAzureLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiAzureLandingZoneResult(
            JOB_ID, LANDING_ZONE_ID, ApiJobReport.StatusEnum.SUCCEEDED);

    when(mockLandingZoneAppService.getCreateAzureLandingZoneResult(any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(GET_CREATE_AZURE_LANDING_ZONE_RESULT + "/{jobId}", JOB_ID), USER_REQUEST))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZone").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZone.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id", Matchers.is(JOB_ID)))
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.landingZone.id", Matchers.is(LANDING_ZONE_ID.toString())));
  }

  @Test
  public void listAzureLandingZoneDefinitionsSuccess() throws Exception {
    ApiAzureLandingZoneDefinitionList definitionList =
        new ApiAzureLandingZoneDefinitionList()
            .landingzones(
                List.of(
                    new ApiAzureLandingZoneDefinition()
                        .definition("fooDefinition")
                        .name("fooName")
                        .description("fooDescription")
                        .version("v1"),
                    new ApiAzureLandingZoneDefinition()
                        .definition("fooDefinition")
                        .name("fooName")
                        .description("fooDescription")
                        .version("v2"),
                    new ApiAzureLandingZoneDefinition()
                        .definition("barDefinition")
                        .name("barName")
                        .description("barDescription")
                        .version("v1")));
    when(mockLandingZoneAppService.listAzureLandingZonesDefinitions(any()))
        .thenReturn(definitionList);

    mockMvc
        .perform(MockMvcUtils.addAuth(get(LIST_AZURE_LANDING_ZONES_DEFINITIONS_PATH), USER_REQUEST))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones").isArray())
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.landingzones", hasSize(definitionList.getLandingzones().size())));
  }

  @Test
  public void deleteAzureLandingZoneSuccess() throws Exception {
    ApiDeleteAzureLandingZoneRequestBody requestBody =
        AzureLandingZoneFixtures.buildDeleteAzureLandingZoneRequest(JOB_ID);

    ApiDeleteAzureLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiDeleteAzureLandingZoneResult(
            JOB_ID, ApiJobReport.StatusEnum.RUNNING, LANDING_ZONE_ID);
    when(mockLandingZoneAppService.deleteLandingZone(any(), any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                post(AZURE_LANDING_ZONE_PATH + "/{landingZoneId}", LANDING_ZONE_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().isAccepted())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZoneId").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.landingZoneId", equalTo(LANDING_ZONE_ID.toString())));
  }

  @ParameterizedTest
  @MethodSource("getDeleteAzureLandingZoneResultScenario")
  public void getDeleteAzureLandingZoneResultSuccess(
      ApiJobReport.StatusEnum jobStatus,
      int expectedHttpStatus,
      ResultMatcher landingZoneMatcher,
      ResultMatcher resourcesMatcher)
      throws Exception {
    ApiDeleteAzureLandingZoneJobResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiDeleteAzureLandingZoneJobResult(
            JOB_ID, LANDING_ZONE_ID, jobStatus);
    when(mockLandingZoneAppService.getDeleteAzureLandingZoneResult(any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(
                        AZURE_LANDING_ZONE_PATH + "/{landingZoneId}/delete-result/{jobId}",
                        LANDING_ZONE_ID,
                        JOB_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().is(expectedHttpStatus))
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id").exists())
        .andExpect(landingZoneMatcher)
        .andExpect(resourcesMatcher);
  }

  @Test
  void listAzureLandingZoneResourcesSuccess() throws Exception {
    ApiAzureLandingZoneResourcesList groupedResources =
        AzureLandingZoneFixtures.buildListLandingZoneResourcesByPurposeResult(LANDING_ZONE_ID);
    when(mockLandingZoneAppService.listAzureLandingZoneResources(any(), any()))
        .thenReturn(groupedResources);
    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(AZURE_LANDING_ZONE_PATH + "/{landingZoneId}/resources", LANDING_ZONE_ID),
                USER_REQUEST))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(LANDING_ZONE_ID.toString())))
        .andExpect(MockMvcResultMatchers.jsonPath("$.resources").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.resources").isArray())
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.resources[0].purpose", Matchers.in(List.of("sharedResources", "lzResources"))))
        .andExpect(MockMvcResultMatchers.jsonPath("$.resources[0].deployedResources").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.resources[0].deployedResources").isArray());
  }

  @Test
  void listAzureLandingZoneResourcesNoResourcesSuccess() throws Exception {
    ApiAzureLandingZoneResourcesList groupedResources =
        AzureLandingZoneFixtures.buildEmptyListLandingZoneResourcesByPurposeResult(LANDING_ZONE_ID);

    when(mockLandingZoneAppService.listAzureLandingZoneResources(any(), any()))
        .thenReturn(groupedResources);
    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(AZURE_LANDING_ZONE_PATH + "/{landingZoneId}/resources", LANDING_ZONE_ID),
                USER_REQUEST))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.resources").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.resources").isArray());
  }

  @Test
  void getAzureLandingZoneByLandingZoneIdSuccess() throws Exception {
    var lzCreateDate = Instant.parse("2024-04-01T12:34:56.789Z").atOffset(ZoneOffset.UTC);
    ApiAzureLandingZone landingZone =
        AzureLandingZoneFixtures.buildDefaultApiAzureLandingZone(
            LANDING_ZONE_ID, BILLING_PROFILE_ID, "definition", "version", lzCreateDate);
    when(mockLandingZoneAppService.getAzureLandingZone(any(), eq(LANDING_ZONE_ID)))
        .thenReturn(landingZone);
    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(AZURE_LANDING_ZONE_PATH + "/{landingZoneId}", LANDING_ZONE_ID), USER_REQUEST))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZoneId").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.landingZoneId", equalTo(LANDING_ZONE_ID.toString())))
        .andExpect(MockMvcResultMatchers.jsonPath("$.definition").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.definition", equalTo("definition")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.version").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.version", equalTo("version")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.billingProfileId").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.billingProfileId", equalTo(BILLING_PROFILE_ID.toString())))
        .andExpect(MockMvcResultMatchers.jsonPath("$.createdDate").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.createdDate", equalTo(lzCreateDate.toString())));
  }

  @Test
  void listAzureLandingZoneByBillingProfileIdSuccess() throws Exception {
    var lzCreateDate = Instant.now().atOffset(ZoneOffset.UTC);
    var landingZone =
        AzureLandingZoneFixtures.buildDefaultApiAzureLandingZone(
            LANDING_ZONE_ID, BILLING_PROFILE_ID, "definition", "version", lzCreateDate);
    ApiAzureLandingZoneList landingZoneList =
        new ApiAzureLandingZoneList().landingzones(List.of(landingZone));

    when(mockLandingZoneAppService.listAzureLandingZones(any(), eq(BILLING_PROFILE_ID)))
        .thenReturn(landingZoneList);
    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(
                    AZURE_LANDING_ZONE_PATH + "?billingProfileId={billingProfileId}",
                    BILLING_PROFILE_ID),
                USER_REQUEST))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones").isArray())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones[0].landingZoneId").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.landingzones[0].landingZoneId", equalTo(LANDING_ZONE_ID.toString())))
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones[0].billingProfileId").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.landingzones[0].billingProfileId", equalTo(BILLING_PROFILE_ID.toString())))
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones[0].definition").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.landingzones[0].definition", equalTo("definition")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones[0].version").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones[0].version", equalTo("version")))
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingzones[0].createdDate").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.landingzones[0].createdDate", equalTo(lzCreateDate.toString())));
  }

  @Test
  void listAzureLandingZoneByBillingProfileIdConflictResponse() throws Exception {
    doThrow(new ConflictException("moreThanOneLandingZoneAssociated"))
        .when(mockLandingZoneAppService)
        .listAzureLandingZones(any(), eq(BILLING_PROFILE_ID));
    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(
                    AZURE_LANDING_ZONE_PATH + "?billingProfileId={billingProfileId}",
                    BILLING_PROFILE_ID),
                USER_REQUEST))
        .andExpect(status().isConflict());
  }

  @Test
  void getAzureLandingZoneByLandingZoneIdUserNotAuthorizedFailed() throws Exception {
    doThrow(new ForbiddenException("User is not authorized to read Landing Zone"))
        .when(mockLandingZoneAppService)
        .getAzureLandingZone(any(), eq(LANDING_ZONE_ID));

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(AZURE_LANDING_ZONE_PATH + "/{landingZoneId}", LANDING_ZONE_ID), USER_REQUEST))
        .andExpect(status().isForbidden());
  }

  private static Stream<Arguments> getDeleteAzureLandingZoneResultScenario() {
    return Stream.of(
        Arguments.of(
            ApiJobReport.StatusEnum.SUCCEEDED,
            HttpStatus.OK.value(),
            MockMvcResultMatchers.jsonPath("$.landingZoneId").exists(),
            MockMvcResultMatchers.jsonPath("$.resources").exists()),
        Arguments.of(
            ApiJobReport.StatusEnum.RUNNING,
            HttpStatus.ACCEPTED.value(),
            MockMvcResultMatchers.jsonPath("$.landingZoneId").doesNotExist(),
            MockMvcResultMatchers.jsonPath("$.resources").doesNotExist()));
  }
}
