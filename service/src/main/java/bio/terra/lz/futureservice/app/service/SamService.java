package bio.terra.lz.futureservice.app.service;

import bio.terra.landingzone.service.iam.LandingZoneSamClient;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.lz.futureservice.generated.model.ApiSystemStatusSystems;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.sam.model.SystemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The purpose of this service is to provide only Sam status information. For other Sam service
 * capabilities see LandingZoneSamService.
 */
@Component
public class SamService extends LandingZoneSamService {
  private static final Logger logger = LoggerFactory.getLogger(SamService.class);

  @Autowired
  public SamService(LandingZoneSamClient samClient) {
    super(samClient);
  }

  public ApiSystemStatusSystems status() {
    // No access token needed since this is an unauthenticated API.
    try {
      // Don't retry status check
      SystemStatus samStatus = getSamClientStatusApi().getSystemStatus();
      var result = new ApiSystemStatusSystems().ok(samStatus.getOk());
      var samSystems = samStatus.getSystems();
      // Populate error message if Sam status is non-ok
      if (result.isOk() == null || !result.isOk()) {
        String errorMsg = "Sam status check failed. Messages = " + samSystems;
        logger.error(errorMsg);
        result.addMessagesItem(errorMsg);
      }
      return result;
    } catch (Exception e) {
      String errorMsg = "Sam status check failed";
      logger.error(errorMsg, e);
      return new ApiSystemStatusSystems().ok(false).messages(List.of(errorMsg));
    }
  }
}
