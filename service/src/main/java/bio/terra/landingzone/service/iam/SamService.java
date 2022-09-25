package bio.terra.landingzone.service.iam;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import bio.terra.common.tracing.OkHttpClientTracingInterceptor;
import bio.terra.landingzone.library.configuration.SamConfiguration;
import io.opencensus.contrib.spring.aop.Traced;
import io.opencensus.trace.Tracing;
import java.util.List;
import java.util.UUID;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.CreateResourceRequestV2;
import org.broadinstitute.dsde.workbench.client.sam.model.FullyQualifiedResourceId;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SamService {
  private static final Logger logger = LoggerFactory.getLogger(SamService.class);
  private final SamConfiguration samConfig;
  private final OkHttpClient commonHttpClient;

  @Autowired
  public SamService(SamConfiguration samConfig) {
    this.samConfig = samConfig;
    this.commonHttpClient =
        new ApiClient()
            .getHttpClient()
            .newBuilder()
            .addInterceptor(new OkHttpClientTracingInterceptor(Tracing.getTracer()))
            .build();
  }

  private ApiClient getApiClient(String accessToken) {
    // OkHttpClient objects manage their own thread pools, so it's much more performant to share one
    // across requests.
    var apiClient =
        new ApiClient().setHttpClient(commonHttpClient).setBasePath(samConfig.getBasePath());
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }

  private ResourcesApi samResourcesApi(String accessToken) {
    return new ResourcesApi(getApiClient(accessToken));
  }

  private UsersApi samUsersApi(String accessToken) {
    return new UsersApi(getApiClient(accessToken));
  }

  /**
   * Checks whether the calling user may perform an action on a Sam resource.
   *
   * @param bearerToken the bearer token of the calling user
   * @param iamResourceType the type of the Sam resource to check
   * @param resourceId the ID of the Sam resource to check
   * @param action the action we're querying Sam for
   * @return true if the user may perform the specified action on the specified resource. False
   *     otherwise.
   */
  @Traced
  public boolean isAuthorized(
      BearerToken bearerToken, String iamResourceType, String resourceId, String action)
      throws InterruptedException {
    var resourceApi = samResourcesApi(bearerToken.getToken());
    try {
      return SamRetry.retry(
          () -> resourceApi.resourcePermissionV2(iamResourceType, resourceId, action));
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error checking resource permission in Sam", apiException);
    }
  }

  /**
   * Wrapper around isAuthorized which throws an appropriate exception if the calling user does not
   * have access to a resource.
   */
  @Traced
  public void checkAuthz(
      BearerToken bearerToken, String resourceType, String resourceId, String action)
      throws InterruptedException {
    final boolean isAuthorized = isAuthorized(bearerToken, resourceType, resourceId, action);
    final String userEmail = getUserStatusInfo(bearerToken).getUserEmail();
    if (!isAuthorized)
      throw new ForbiddenException(
          String.format(
              "User %s is not authorized to %s resource %s of type %s",
              userEmail, action, resourceId, resourceType));
    else
      logger.info(
          "User {} is authorized to {} resource {} of type {}",
          userEmail,
          action,
          resourceId,
          resourceType);
  }

  /**
   * Creates a landing-zone resource in Sam with a parent billing profile, and default owner policy.
   *
   * @param bearerToken the bearer token of the calling user
   * @param billingProfileId the ID of the billing profile to set as the parent Sam resource. The
   *     effect of this is that the landing zone inherits permissions of the billing profile.
   * @param landingZoneId the ID of the landing zone resource to create
   */
  @Traced
  public void createLandingZone(BearerToken bearerToken, UUID billingProfileId, UUID landingZoneId)
      throws InterruptedException {
    var resourceApi = samResourcesApi(bearerToken.getToken());

    var parentId =
        new FullyQualifiedResourceId()
            .resourceId(billingProfileId.toString())
            .resourceTypeName(SamConstants.SamResourceType.SPEND_PROFILE);

    var landingZoneRequest =
        new CreateResourceRequestV2()
            .resourceId(landingZoneId.toString())
            .parent(parentId)
            .authDomain(List.of());
    try {
      SamRetry.retry(
          () ->
              resourceApi.createResourceV2(
                  SamConstants.SamResourceType.LANDING_ZONE, landingZoneRequest));
      logger.info("Created Sam resource for landing zone {}", landingZoneId);
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create(
          "Error creating a landing zone resource in Sam", apiException);
    }
  }

  /**
   * Deletes a landing-zone resource in Sam.
   *
   * @param bearerToken bearer token of the calling user
   * @param landingZoneId the ID of the landing zone resource to delete
   */
  @Traced
  public void deleteLandingZone(BearerToken bearerToken, UUID landingZoneId)
      throws InterruptedException {
    var resourceApi = samResourcesApi(bearerToken.getToken());
    try {
      SamRetry.retry(
          () ->
              resourceApi.deleteResourceV2(
                  SamConstants.SamResourceType.LANDING_ZONE, landingZoneId.toString()));
      logger.info("Deleted Sam resource for landing zone {}", landingZoneId);
    } catch (ApiException apiException) {
      logger.info("Sam API error while deleting landing zone, code is " + apiException.getCode());
      // Do nothing if the resource to delete is not found, this may not be the first time undo is
      // called. Other exceptions still need to be surfaced.
      if (apiException.getCode() == HttpStatus.NOT_FOUND.value()) {
        logger.info(
            "Sam error was NOT_FOUND on a deletion call. "
                + "This just means the deletion was tried twice so no error thrown.");
        return;
      }
      throw SamExceptionFactory.create("Error deleting a landing zone in Sam", apiException);
    }
  }

  /** Fetch the user status info associated with the user credentials directly from Sam. */
  private UserStatusInfo getUserStatusInfo(BearerToken bearerToken) throws InterruptedException {
    var usersApi = samUsersApi(bearerToken.getToken());
    try {
      return SamRetry.retry(usersApi::getUserStatusInfo);
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error getting user status info from Sam", apiException);
    }
  }
}
