package bio.terra.lz.futureservice.app.service.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import bio.terra.lz.futureservice.app.configuration.StatusCheckConfiguration;
import bio.terra.lz.futureservice.common.fixture.SystemStatusFixtures;
import bio.terra.lz.futureservice.generated.model.ApiSystemStatusSystems;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class StatusServiceTest {

  @Mock private SamStatusService mockSamStatusService;
  @Mock private NamedParameterJdbcTemplate mockNamedParameterJdbcTemplate;
  @Mock private JdbcTemplate mockJdbcTemplate;

  private StatusCheckConfiguration mockStatusCheckConfiguration;

  private StatusService statusService;

  @BeforeEach
  void setup() {
    mockStatusCheckConfiguration = new StatusCheckConfiguration(true, 0, 0, 1);
    statusService =
        new StatusService(
            mockSamStatusService, mockNamedParameterJdbcTemplate, mockStatusCheckConfiguration);
  }

  @Test
  void testGetCurrentStatus_Success() {
    var samStatus = new ApiSystemStatusSystems().ok(true);
    when(mockSamStatusService.status()).thenReturn(samStatus);
    when(mockJdbcTemplate.execute(ArgumentMatchers.<ConnectionCallback<Boolean>>any()))
        .thenReturn(true);
    when(mockNamedParameterJdbcTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);

    statusService.checkStatus();

    var expectedStatus = SystemStatusFixtures.buildOKSystemStatus();
    assertEquals(expectedStatus, statusService.getCurrentStatus());
  }

  @Test
  void testGetCurrentStatus_DbSubsystemReturnsConnectionFailure() {
    var samStatus = new ApiSystemStatusSystems().ok(true);
    when(mockSamStatusService.status()).thenReturn(samStatus);
    when(mockJdbcTemplate.execute(ArgumentMatchers.<ConnectionCallback<Boolean>>any()))
        .thenReturn(false);
    when(mockNamedParameterJdbcTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);

    statusService.checkStatus();

    var expectedStatus = SystemStatusFixtures.buildWithCustomStatuses(false, true);
    assertEquals(expectedStatus, statusService.getCurrentStatus());
  }
}
