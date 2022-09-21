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
    ApiClient apiClient =
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

  @Traced
  public boolean isAuthorized(
      BearerToken bearerToken, String iamResourceType, String resourceId, String action)
      throws InterruptedException {
    ResourcesApi resourceApi = samResourcesApi(bearerToken.getToken());
    try {
      return SamRetry.retry(
          () -> resourceApi.resourcePermissionV2(iamResourceType, resourceId, action));
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error checking resource permission in Sam", apiException);
    }
  }

  @Traced
  public void checkAuthz(
      BearerToken bearerToken, String resourceType, String resourceId, String action)
      throws InterruptedException {
    boolean isAuthorized = isAuthorized(bearerToken, resourceType, resourceId, action);
    final String userEmail = getUserEmailFromSam(bearerToken);
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

  @Traced
  public void createLandingZone(
      BearerToken bearerToken, String billingprofileId, String landingZoneId)
      throws InterruptedException {
    ResourcesApi resourceApi = samResourcesApi(bearerToken.getToken());

    FullyQualifiedResourceId billingProfileParentId =
        new FullyQualifiedResourceId()
            .resourceId(billingprofileId)
            .resourceTypeName(SamConstants.SamResourceType.SPEND_PROFILE);

    CreateResourceRequestV2 landingZoneRequest =
        new CreateResourceRequestV2()
            .resourceId(landingZoneId)
            .parent(billingProfileParentId)
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

  @Traced
  public void deleteLandingZone(BearerToken bearerToken, String landingZoneId)
      throws InterruptedException {
    ResourcesApi resourceApi = samResourcesApi(bearerToken.getToken());
    try {
      SamRetry.retry(
          () ->
              resourceApi.deleteResourceV2(
                  SamConstants.SamResourceType.LANDING_ZONE, landingZoneId));
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

  /**
   * Fetch the email associated with user credentials directly from Sam. Unlike {@code
   * getRequestUserEmail}, this will always call Sam to fetch an email and will never read it from
   * the AuthenticatedUserRequest. This is important for calls made by pet service accounts, which
   * will have a pet email in the AuthenticatedUserRequest, but Sam will return the owner's email.
   */
  private String getUserEmailFromSam(BearerToken bearerToken) throws InterruptedException {
    return getUserStatusInfo(bearerToken).getUserEmail();
  }

  /** Fetch the user status info associated with the user credentials directly from Sam. */
  private UserStatusInfo getUserStatusInfo(BearerToken bearerToken) throws InterruptedException {
    UsersApi usersApi = samUsersApi(bearerToken.getToken());
    try {
      return SamRetry.retry(usersApi::getUserStatusInfo);
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error getting user status info from Sam", apiException);
    }
  }
}
