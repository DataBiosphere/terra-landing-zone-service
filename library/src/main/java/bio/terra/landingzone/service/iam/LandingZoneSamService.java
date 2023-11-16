package bio.terra.landingzone.service.iam;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.sam.SamRetry;
import bio.terra.common.sam.exception.SamExceptionFactory;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import io.opencensus.contrib.spring.aop.Traced;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyMembershipV2;
import org.broadinstitute.dsde.workbench.client.sam.model.CreateResourceRequestV2;
import org.broadinstitute.dsde.workbench.client.sam.model.FullyQualifiedResourceId;
import org.broadinstitute.dsde.workbench.client.sam.model.UserResourcesResponse;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneSamService {
  private static final Logger logger = LoggerFactory.getLogger(LandingZoneSamService.class);
  private final LandingZoneSamClient samClient;
  public static final String IS_AUTHORIZED = "isAuthorized";

  @Autowired
  public LandingZoneSamService(LandingZoneSamClient samClient) {
    this.samClient = samClient;
  }

  protected StatusApi getSamClientStatusApi() {
    return samClient.statusApi();
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
    var resourceApi = samClient.resourcesApi(bearerToken.getToken());
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
    if (!isAuthorized) {
      final String userEmail = getUserStatusInfo(bearerToken).getUserEmail();
      throw new ForbiddenException(
          String.format(
              "User %s is not authorized perform action %s on resource %s of type %s",
              userEmail, action, resourceId, resourceType));
    }
  }

  @Traced
  public void checkUserEnabled(BearerToken bearerToken) throws InterruptedException {
    var userInfo = getUserStatusInfo(bearerToken);
    if (!BooleanUtils.isTrue(userInfo.getEnabled())) {
      throw new UnauthorizedException("User is disabled");
    }
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
    var userInfo = getUserStatusInfo(bearerToken);
    var resourcesApi = samClient.resourcesApi(bearerToken.getToken());

    var parentId =
        new FullyQualifiedResourceId()
            .resourceId(billingProfileId.toString())
            .resourceTypeName(SamConstants.SamResourceType.SPEND_PROFILE);

    Map<String, AccessPolicyMembershipV2> policies = new HashMap<>();
    policies.put(
        "owner",
        new AccessPolicyMembershipV2()
            .addMemberEmailsItem(userInfo.getUserEmail())
            .addRolesItem(SamConstants.SamRole.OWNER));
    if (CollectionUtils.isNotEmpty(samClient.getLandingZoneResourceUsers())) {
      policies.put(
          "user",
          new AccessPolicyMembershipV2()
              .memberEmails(samClient.getLandingZoneResourceUsers())
              .addRolesItem(SamConstants.SamRole.USER));
    }
    var landingZoneRequest =
        new CreateResourceRequestV2()
            .resourceId(landingZoneId.toString())
            .policies(policies)
            .parent(parentId)
            .authDomain(List.of());
    try {
      SamRetry.retry(
          () ->
              resourcesApi.createResourceV2(
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
    var resourceApi = samClient.resourcesApi(bearerToken.getToken());
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

  @Traced
  public List<UUID> listLandingZoneResourceIds(BearerToken bearerToken)
      throws InterruptedException {
    var resourceApi = samClient.resourcesApi(bearerToken.getToken());
    try {
      List<UserResourcesResponse> userLandingZones =
          SamRetry.retry(
              () ->
                  resourceApi.listResourcesAndPoliciesV2(
                      SamConstants.SamResourceType.LANDING_ZONE));
      return userLandingZones.stream()
          .flatMap(
              p -> {
                try {
                  return Stream.of(UUID.fromString(p.getResourceId()));
                } catch (IllegalArgumentException e) {
                  return Stream.empty();
                }
              })
          .toList();
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error getting landing zone ID's in Sam", apiException);
    }
  }

  /** Fetch the user status info associated with the user credentials directly from Sam. */
  private UserStatusInfo getUserStatusInfo(BearerToken bearerToken) throws InterruptedException {
    var usersApi = samClient.usersApi(bearerToken.getToken());
    try {
      return SamRetry.retry(usersApi::getUserStatusInfo);
    } catch (ApiException apiException) {
      throw SamExceptionFactory.create("Error getting user status info from Sam", apiException);
    }
  }
}
