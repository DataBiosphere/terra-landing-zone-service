package bio.terra.lz.futureservice.app.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.lz.futureservice.app.configuration.VersionConfiguration;
import bio.terra.lz.futureservice.app.service.status.StatusService;
import bio.terra.lz.futureservice.common.fixture.SystemStatusFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
public class PublicApiControllerTest {
  private static final String build = "buildInfo";
  private static final String gitHash = "hash";
  private static final String gitTag = "tag";

  private static final String AZURE_SYSTEM_VERSION_PATH = "/version";
  private static final String AZURE_SYSTEM_STATUS_PATH = "/status";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean StatusService mockStatusService;

  @TestConfiguration
  // this configuration is used only by testServiceVersionSuccess test.
  static class TestConfig {
    @Bean
    @Primary
    public VersionConfiguration versionConfigurationTest() {
      var mockVersionConfiguration = mock(VersionConfiguration.class);
      when(mockVersionConfiguration.getBuild()).thenReturn(build);
      when(mockVersionConfiguration.getGitHash()).thenReturn(gitHash);
      when(mockVersionConfiguration.getGitTag()).thenReturn(gitTag);
      return mockVersionConfiguration;
    }
  }

  @Test
  public void testServiceVersionSuccess() throws Exception {
    mockMvc
        .perform(get(AZURE_SYSTEM_VERSION_PATH))
        .andExpect(status().is(HttpStatus.SC_OK))
        .andExpect(MockMvcResultMatchers.jsonPath("$.gitTag").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.gitTag", Matchers.is(gitTag)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.gitHash").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.gitHash", Matchers.is(gitHash)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.github").exists())
        .andExpect(
            MockMvcResultMatchers.jsonPath(
                "$.github", Matchers.is(PublicApiController.LANDING_ZONE_REPO_URL + gitHash)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.build").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.build", Matchers.is(build)));
  }

  @Test
  public void testServiceStatusSuccess() throws Exception {
    var status = SystemStatusFixtures.buildOKSystemStatus();

    when(mockStatusService.getCurrentStatus()).thenReturn(status);

    mockMvc
        .perform(get(AZURE_SYSTEM_STATUS_PATH))
        .andExpect(status().is(HttpStatus.SC_OK))
        .andExpect(MockMvcResultMatchers.jsonPath("$.ok").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.ok", Matchers.is(true)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Database").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Database.ok").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Database.ok", Matchers.is(true)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Sam").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Sam.ok").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Sam.ok", Matchers.is(true)));
  }

  @Test
  public void testServiceStatusServiceUnavailable() throws Exception {
    var status = SystemStatusFixtures.buildSystemStatusWithSomeFailure();

    when(mockStatusService.getCurrentStatus()).thenReturn(status);

    mockMvc
        .perform(get(AZURE_SYSTEM_STATUS_PATH))
        .andExpect(status().is(HttpStatus.SC_SERVICE_UNAVAILABLE))
        .andExpect(MockMvcResultMatchers.jsonPath("$.ok").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.ok", Matchers.is(false)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Database").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Database.ok").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Database.ok", Matchers.is(false)))
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Sam").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Sam.ok").exists())
        .andExpect(MockMvcResultMatchers.jsonPath("$.systems.Sam.ok", Matchers.is(true)));
  }
}
