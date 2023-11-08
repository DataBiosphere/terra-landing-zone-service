package bio.terra.lz.futureservice.app.controller;

import static bio.terra.lz.futureservice.common.utils.MockMvcUtils.USER_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.lz.futureservice.app.service.LandingZoneAppService;
import bio.terra.lz.futureservice.common.fixture.AzureLandingZoneFixtures;
import bio.terra.lz.futureservice.common.utils.MockMvcUtils;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateAzureLandingZoneRequestBody;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiJobReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
public class LandingZoneApiControllerTest {
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

  @MockBean LandingZoneAppService landingZoneAppService;

  @Test
  public void createAzureLandingZoneJobRunning() throws Exception {
    ApiCreateLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiCreateLandingZoneSuccessResult(JOB_ID);
    ApiCreateAzureLandingZoneRequestBody requestBody =
        AzureLandingZoneFixtures.buildCreateAzureLandingZoneRequest(JOB_ID, BILLING_PROFILE_ID);

    when(landingZoneAppService.createAzureLandingZone(any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                post(AZURE_LANDING_ZONE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().is(HttpStatus.SC_ACCEPTED))
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

    when(landingZoneAppService.createAzureLandingZone(any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                post(AZURE_LANDING_ZONE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().is(HttpStatus.SC_BAD_REQUEST));
  }

  @Test
  public void createAzureLandingZoneWithoutBillingProfileValidationFailed() throws Exception {
    ApiCreateLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.buildApiCreateLandingZoneSuccessResult(JOB_ID);
    ApiCreateAzureLandingZoneRequestBody requestBody =
        AzureLandingZoneFixtures.buildCreateAzureLandingZoneRequestWithoutBillingProfile(JOB_ID);

    when(landingZoneAppService.createAzureLandingZone(any(), any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                post(AZURE_LANDING_ZONE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
                    .characterEncoding("utf-8"),
                USER_REQUEST))
        .andExpect(status().is(HttpStatus.SC_BAD_REQUEST));
  }

  @Test
  public void getCreateAzureLandingZoneResultJobRunning() throws Exception {
    ApiAzureLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.createApiAzureLandingZoneResult(
            JOB_ID, ApiJobReport.StatusEnum.RUNNING);

    when(landingZoneAppService.getCreateAzureLandingZoneResult(any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(GET_CREATE_AZURE_LANDING_ZONE_RESULT + "/{jobId}", JOB_ID), USER_REQUEST))
        .andExpect(status().is(HttpStatus.SC_ACCEPTED))
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id", Matchers.is(JOB_ID)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZone").doesNotExist());
  }

  @Test
  public void getCreateAzureLandingZoneResultJobSucceeded() throws Exception {
    ApiAzureLandingZoneResult asyncJobResult =
        AzureLandingZoneFixtures.createApiAzureLandingZoneResult(
            JOB_ID, LANDING_ZONE_ID, ApiJobReport.StatusEnum.SUCCEEDED);

    when(landingZoneAppService.getCreateAzureLandingZoneResult(any(), any()))
        .thenReturn(asyncJobResult);

    mockMvc
        .perform(
            MockMvcUtils.addAuth(
                get(GET_CREATE_AZURE_LANDING_ZONE_RESULT + "/{jobId}", JOB_ID), USER_REQUEST))
        .andExpect(status().is(HttpStatus.SC_OK))
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZone").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.landingZone.id").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.jobReport.id", Matchers.is(JOB_ID)))
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.landingZone.id", Matchers.is(LANDING_ZONE_ID.toString())));
  }
}
