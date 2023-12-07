package bio.terra.landingzone.job;

import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.stairway.MonitoringHook;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.landingzone.job.exception.InvalidJobIdException;
import bio.terra.landingzone.job.exception.InvalidJobParameterException;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneRequest;
import bio.terra.landingzone.stairway.common.utils.LandingZoneMdcHook;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class LandingZoneJobBuilder {
  private final LandingZoneJobService jobService;
  private final StairwayComponent stairwayComponent;
  private final LandingZoneMdcHook mdcHook;
  private final FlightMap jobParameterMap;
  private final OpenTelemetry openTelemetry;
  private Class<? extends Flight> flightClass;
  @Nullable private String jobId;
  @Nullable private String description;
  @Nullable private LandingZoneRequest landingZoneRequest;
  @Nullable private OperationType operationType;
  @Nullable private BearerToken bearerToken;

  public LandingZoneJobBuilder(
      LandingZoneJobService jobService,
      StairwayComponent stairwayComponent,
      LandingZoneMdcHook mdcHook,
      OpenTelemetry openTelemetry) {
    this.jobService = jobService;
    this.stairwayComponent = stairwayComponent;
    this.mdcHook = mdcHook;
    this.openTelemetry = openTelemetry;
    this.jobParameterMap = new FlightMap();
  }

  public LandingZoneJobBuilder flightClass(Class<? extends Flight> flightClass) {
    this.flightClass = flightClass;
    return this;
  }

  public LandingZoneJobBuilder jobId(@Nullable String jobId) {
    // If clients provide a non-null job ID, it cannot be whitespace-only
    if (StringUtils.isWhitespace(jobId)) {
      throw new InvalidJobIdException("jobId cannot be whitespace-only.");
    }
    this.jobId = jobId;
    return this;
  }

  public LandingZoneJobBuilder description(@Nullable String description) {
    this.description = description;
    return this;
  }

  public LandingZoneJobBuilder landingZoneRequest(@Nullable LandingZoneRequest landingZoneRequest) {
    this.landingZoneRequest = landingZoneRequest;
    return this;
  }

  public LandingZoneJobBuilder operationType(@Nullable OperationType operationType) {
    this.operationType = operationType;
    return this;
  }

  public LandingZoneJobBuilder bearerToken(@Nullable BearerToken bearerToken) {
    this.bearerToken = bearerToken;
    return this;
  }

  public LandingZoneJobBuilder addParameter(String keyName, @Nullable Object val) {
    if (StringUtils.isBlank(keyName)) {
      throw new InvalidJobParameterException("Parameter name cannot be null or blanks.");
    }
    // note that this call overwrites a parameter if it already exists
    jobParameterMap.put(keyName, val);
    return this;
  }

  /**
   * Submit a job to stairway and return the jobID immediately.
   *
   * @return jobID of submitted flight
   */
  public String submit() {
    populateInputParams();
    return jobService.submit(flightClass, jobParameterMap, jobId);
  }

  /**
   * Submit a job to stairway, wait until it's complete, and return the job result.
   *
   * @param resultClass Class of the job's result
   * @return Result of the finished job.
   */
  @WithSpan
  public <T> T submitAndWait(Class<T> resultClass) {
    populateInputParams();
    return jobService.submitAndWait(flightClass, jobParameterMap, resultClass, jobId);
  }

  // Check the inputs, supply defaults and finalize the input parameter map
  private void populateInputParams() {
    if (flightClass == null) {
      throw new MissingRequiredFieldException("Missing flight class: flightClass");
    }

    if (operationType == null || operationType == OperationType.UNKNOWN) {
      throw new MissingRequiredFieldException("Missing or unspecified operation type");
    }

    if (bearerToken == null) {
      throw new MissingRequiredFieldException("Missing bearer token");
    }

    // Default to a generated job id
    if (jobId == null) {
      jobId = stairwayComponent.get().createFlightId();
    }

    // Always add the MDC logging and tracing span parameters for the mdc hook
    addParameter(LandingZoneMdcHook.MDC_FLIGHT_MAP_KEY, mdcHook.getSerializedCurrentContext());
    addParameter(
        MonitoringHook.SUBMISSION_SPAN_CONTEXT_MAP_KEY,
        MonitoringHook.serializeCurrentTracingContext(openTelemetry));

    // Convert any other members that were set into parameters. However, if they were
    // explicitly added with addParameter during construction, we do not overwrite them.
    if (shouldInsert(JobMapKeys.DESCRIPTION, description)) {
      addParameter(JobMapKeys.DESCRIPTION.getKeyName(), description);
    }

    if (shouldInsert(LandingZoneFlightMapKeys.OPERATION_TYPE, operationType)) {
      addParameter(LandingZoneFlightMapKeys.OPERATION_TYPE, operationType);
    }

    if (shouldInsert(LandingZoneFlightMapKeys.BEARER_TOKEN, bearerToken)) {
      addParameter(LandingZoneFlightMapKeys.BEARER_TOKEN, bearerToken);
    }
  }

  private boolean shouldInsert(String mapKey, @Nullable Object value) {
    return (value != null && !jobParameterMap.containsKey(mapKey));
  }

  private boolean shouldInsert(JobMapKeys mapKey, @Nullable Object value) {
    return (value != null && !jobParameterMap.containsKey(mapKey.getKeyName()));
  }
}
