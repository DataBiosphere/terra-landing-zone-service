package bio.terra.lz.futureservice.common.utils;

import bio.terra.landingzone.job.model.ErrorReport;
import bio.terra.landingzone.job.model.JobReport;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapperUtils {
  public static class LandingZoneMapper {
    private LandingZoneMapper() {}

    public static HashMap<String, String> landingZoneParametersFrom(
        List<ApiAzureLandingZoneParameter> parametersList) {
      return nullSafeListToStream(parametersList)
          .flatMap(Stream::ofNullable)
          .collect(
              Collectors.toMap(
                  ApiAzureLandingZoneParameter::getKey,
                  ApiAzureLandingZoneParameter::getValue,
                  (prev, next) -> prev,
                  HashMap::new));
    }

    private static <T> Stream<T> nullSafeListToStream(Collection<T> collection) {
      return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
    }
  }

  public static class JobReportMapper {
    private JobReportMapper() {}

    public static bio.terra.lz.futureservice.generated.model.ApiJobReport from(
        JobReport jobReport) {
      return new bio.terra.lz.futureservice.generated.model.ApiJobReport()
          .id(jobReport.getId())
          .description(jobReport.getDescription())
          .status(
              bio.terra.lz.futureservice.generated.model.ApiJobReport.StatusEnum.valueOf(
                  jobReport.getStatus().toString()))
          .statusCode(jobReport.getStatusCode())
          .submitted(jobReport.getSubmitted())
          .completed(jobReport.getCompleted())
          .resultURL(jobReport.getResultURL());
    }
  }

  public static class ErrorReportMapper {
    private ErrorReportMapper() {}

    public static bio.terra.lz.futureservice.generated.model.ApiErrorReport from(
        ErrorReport errorReport) {
      if (errorReport == null) {
        return null;
      }
      return new bio.terra.lz.futureservice.generated.model.ApiErrorReport()
          .message(errorReport.getMessage())
          .statusCode(errorReport.getStatusCode())
          .causes(errorReport.getCauses());
    }
  }
}
