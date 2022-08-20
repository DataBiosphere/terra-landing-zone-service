package bio.terra.landingzone.job;

import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.common.stairway.TracingHook;
import bio.terra.landingzone.job.exception.InvalidJobIdException;
import bio.terra.landingzone.job.exception.InvalidJobParameterException;
import bio.terra.landingzone.job.model.OperationType;
import bio.terra.landingzone.model.AuthenticatedUserRequest;
import bio.terra.landingzone.resource.ExternalResourceType;
import bio.terra.landingzone.resource.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.resource.landingzone.ExternalLandingZoneResource;
import bio.terra.landingzone.resource.model.StewardshipType;
import bio.terra.landingzone.stairway.common.utils.LandingZoneMdcHook;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import io.opencensus.contrib.spring.aop.Traced;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class AzureLandingZoneJobBuilder {
  private final AzureLandingZoneJobService jobService;
  private final StairwayComponent stairwayComponent;
  private final LandingZoneMdcHook mdcHook;
  private final FlightMap jobParameterMap;
  private Class<? extends Flight> flightClass;
  @Nullable private String jobId;
  @Nullable private String description;
  @Nullable private Object request;
  @Nullable private AuthenticatedUserRequest userRequest;
  // Well-known keys used for filtering workspace jobs
  // All applicable ones of these should be supplied on every flight
  //  private String workspaceId;
  @Nullable private ExternalLandingZoneResource resource;
  @Nullable private ExternalResourceType resourceType;
  @Nullable private String resourceName;
  @Nullable private StewardshipType stewardshipType;
  @Nullable private OperationType operationType;

  public AzureLandingZoneJobBuilder(
      AzureLandingZoneJobService jobService,
      StairwayComponent stairwayComponent,
      LandingZoneMdcHook mdcHook) {
    this.jobService = jobService;
    this.stairwayComponent = stairwayComponent;
    this.mdcHook = mdcHook;
    this.jobParameterMap = new FlightMap();
  }

  public AzureLandingZoneJobBuilder flightClass(Class<? extends Flight> flightClass) {
    this.flightClass = flightClass;
    return this;
  }

  public AzureLandingZoneJobBuilder jobId(@Nullable String jobId) {
    // If clients provide a non-null job ID, it cannot be whitespace-only
    if (StringUtils.isWhitespace(jobId)) {
      throw new InvalidJobIdException("jobId cannot be whitespace-only.");
    }
    this.jobId = jobId;
    return this;
  }

  public AzureLandingZoneJobBuilder description(@Nullable String description) {
    this.description = description;
    return this;
  }

  public AzureLandingZoneJobBuilder request(@Nullable Object request) {
    this.request = request;
    return this;
  }

  public AzureLandingZoneJobBuilder userRequest(@Nullable AuthenticatedUserRequest userRequest) {
    this.userRequest = userRequest;
    return this;
  }

  //  public AzureLandingZoneJobBuilder workspaceId(@Nullable String workspaceId) {
  //    this.workspaceId = workspaceId;
  //    return this;
  //  }

  public AzureLandingZoneJobBuilder resource(@Nullable ExternalLandingZoneResource resource) {
    this.resource = resource;
    return this;
  }

  public AzureLandingZoneJobBuilder resourceType(@Nullable ExternalResourceType resourceType) {
    this.resourceType = resourceType;
    return this;
  }

  public AzureLandingZoneJobBuilder resourceName(@Nullable String resourceName) {
    this.resourceName = resourceName;
    return this;
  }

  public AzureLandingZoneJobBuilder stewardshipType(@Nullable StewardshipType stewardshipType) {
    this.stewardshipType = stewardshipType;
    return this;
  }

  public AzureLandingZoneJobBuilder operationType(@Nullable OperationType operationType) {
    this.operationType = operationType;
    return this;
  }

  public AzureLandingZoneJobBuilder addParameter(String keyName, @Nullable Object val) {
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
  @Traced
  public <T> T submitAndWait(Class<T> resultClass) {
    populateInputParams();
    return jobService.submitAndWait(flightClass, jobParameterMap, resultClass, jobId);
  }

  // Check the inputs, supply defaults and finalize the input parameter map
  private void populateInputParams() {
    if (flightClass == null) {
      throw new MissingRequiredFieldException("Missing flight class: flightClass");
    }
    //
    //    if (workspaceId == null) {
    //      throw new MissingRequiredFieldException("Missing workspace ID");
    //    }

    if (operationType == null || operationType == OperationType.UNKNOWN) {
      throw new MissingRequiredFieldException("Missing or unspecified operation type");
    }

    // Default to a generated job id
    if (jobId == null) {
      jobId = stairwayComponent.get().createFlightId();
    }

    // Always add the MDC logging and tracing span parameters for the mdc hook
    addParameter(LandingZoneMdcHook.MDC_FLIGHT_MAP_KEY, mdcHook.getSerializedCurrentContext());
    addParameter(
        TracingHook.SUBMISSION_SPAN_CONTEXT_MAP_KEY, TracingHook.serializeCurrentTracingContext());

    // Convert any other members that were set into parameters. However, if they were
    // explicitly added with addParameter during construction, we do not overwrite them.
    if (shouldInsert(JobMapKeys.DESCRIPTION, description)) {
      addParameter(JobMapKeys.DESCRIPTION.getKeyName(), description);
    }
    if (shouldInsert(JobMapKeys.REQUEST, request)) {
      addParameter(JobMapKeys.REQUEST.getKeyName(), request);
    }
    if (shouldInsert(JobMapKeys.AUTH_USER_INFO, userRequest)) {
      addParameter(JobMapKeys.AUTH_USER_INFO.getKeyName(), userRequest);
      addParameter(JobMapKeys.SUBJECT_ID.getKeyName(), userRequest.getSubjectId());
    }
    //    if (shouldInsert(WorkspaceFlightMapKeys.WORKSPACE_ID, workspaceId)) {
    //      addParameter(WorkspaceFlightMapKeys.WORKSPACE_ID, workspaceId);
    //    }
    if (shouldInsert(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE, resource)) {
      addParameter(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE, resource);
    }
    if (shouldInsert(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE_TYPE, resourceType)) {
      addParameter(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE_TYPE, resourceType);
    }
    if (shouldInsert(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE_NAME, resourceName)) {
      addParameter(LandingZoneFlightMapKeys.ResourceKeys.RESOURCE_NAME, resourceName);
    }
    if (shouldInsert(LandingZoneFlightMapKeys.ResourceKeys.STEWARDSHIP_TYPE, stewardshipType)) {
      addParameter(LandingZoneFlightMapKeys.ResourceKeys.STEWARDSHIP_TYPE, stewardshipType);
    }
    if (shouldInsert(LandingZoneFlightMapKeys.OPERATION_TYPE, operationType)) {
      addParameter(LandingZoneFlightMapKeys.OPERATION_TYPE, operationType);
    }
  }

  private boolean shouldInsert(String mapKey, @Nullable Object value) {
    return (value != null && !jobParameterMap.containsKey(mapKey));
  }

  private boolean shouldInsert(JobMapKeys mapKey, @Nullable Object value) {
    return (value != null && !jobParameterMap.containsKey(mapKey.getKeyName()));
  }
}
