package bio.terra.lz.futureservice.app.service.status;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.service.iam.LandingZoneSamClient;
import java.util.Map;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.model.SubsystemStatus;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SamStatusServiceTest {

  @Mock private LandingZoneSamClient mockLandingZoneSamClient;
  @Mock private StatusApi mockSamClientStatusApi;

  private SamStatusService samStatusService;

  @BeforeEach
  void setup() {
    samStatusService = new SamStatusService(mockLandingZoneSamClient);
  }

  @Test
  void testCheckStatus_Success() throws ApiException {
    var systems = Map.of("Db", new SubsystemStatus().ok(true));
    var systemStatus = new SystemStatus().ok(true).systems(systems);
    when(mockSamClientStatusApi.getSystemStatus()).thenReturn(systemStatus);
    when(mockLandingZoneSamClient.statusApi()).thenReturn(mockSamClientStatusApi);

    assertTrue(samStatusService.status().isOk());
  }

  @Test
  void testCheckStatus_SamSubsystemStatusReturnsFailure() throws ApiException {
    var systems = Map.of("Db", new SubsystemStatus().ok(false));
    var systemStatus = new SystemStatus().ok(false).systems(systems);
    when(mockSamClientStatusApi.getSystemStatus()).thenReturn(systemStatus);
    when(mockLandingZoneSamClient.statusApi()).thenReturn(mockSamClientStatusApi);

    var status = samStatusService.status();
    assertFalse(status.isOk());
    assertTrue(status.getMessages().stream().anyMatch(m -> m.contains("check failed")));
  }

  @Test
  void testCheckStatus_FailedToGetSystemStatus() throws ApiException {
    when(mockLandingZoneSamClient.statusApi()).thenReturn(mockSamClientStatusApi);
    doThrow(ApiException.class).when(mockSamClientStatusApi).getSystemStatus();

    var status = samStatusService.status();
    assertFalse(status.isOk());
    assertTrue(status.getMessages().stream().anyMatch(m -> m.contains("check failed")));
  }
}
