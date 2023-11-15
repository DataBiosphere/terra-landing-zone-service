package bio.terra.lz.futureservice.app.controller.common;

import bio.terra.lz.futureservice.generated.model.ApiJobReport;
import org.springframework.http.HttpStatus;

public class ResponseUtils {
  private ResponseUtils() {}

  /**
   * Return the appropriate response code for an endpoint, given an async job report. For a job
   * that's still running, this is 202. For a job that's finished (either succeeded or failed), the
   * endpoint should return 200. More informational status codes will be included in either the
   * response or error report bodies.
   */
  public static HttpStatus getAsyncResponseCode(ApiJobReport jobReport) {
    return jobReport.getStatus() == ApiJobReport.StatusEnum.RUNNING
        ? HttpStatus.ACCEPTED
        : HttpStatus.OK;
  }
}
