package bio.terra.lz.futureservice.app.service.status;

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
 * capabilities see LandingZoneSamService. We can't implement this status functionality there since
 * LandingZoneSamService is located in library module and doesn't have access to Api* models. Most
 * likely later library module functionality migrate into service modules, and this status check can
 * be moved into proper location.
 */
@Component
public class SamStatusService {
  private static final Logger logger = LoggerFactory.getLogger(SamStatusService.class);

  private final LandingZoneSamService landingZoneSamService;

  @Autowired
  public SamStatusService(LandingZoneSamService landingZoneSamService) {
    this.landingZoneSamService = landingZoneSamService;
  }

  public ApiSystemStatusSystems status() {
    // No access token needed since this is an unauthenticated API.
    try {
      // Don't retry status check
      SystemStatus samStatus = landingZoneSamService.getSamClientStatusApi().getSystemStatus();
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
