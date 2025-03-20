package bio.terra.landingzone.service.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerToken;
import bio.terra.common.iam.SamUser;
import bio.terra.common.sam.exception.SamInternalServerErrorException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyMembershipRequest;
import org.broadinstitute.dsde.workbench.client.sam.model.CreateResourceRequestV2;
import org.broadinstitute.dsde.workbench.client.sam.model.FullyQualifiedResourceId;
import org.broadinstitute.dsde.workbench.client.sam.model.RolesAndActions;
import org.broadinstitute.dsde.workbench.client.sam.model.UserResourcesResponse;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LandingZoneSamServiceTest {
  private static final SamUser SAM_USER =
      new SamUser("test@example.com", "Subject", new BearerToken("0123.456-789AbCd"));
  private static final String RESOURCE_TYPE = "resource_type";
  private static final String RESOURCE_ID = "resource_id";
  private static final String RESOURCE_ACTION = "action";
  private static final UUID LANDING_ZONE_ID = UUID.randomUUID();
  private static final UUID BILLING_PROFILE_ID = UUID.randomUUID();
  private static final ApiException API_EXCEPTION =
      new ApiException("Message: SomeError\nHTTP response code: 500");
  private LandingZoneSamService samService;

  @Mock private LandingZoneSamClient samClient;
  @Mock private ResourcesApi resourcesApi;
  @Mock private UsersApi usersApi;
  @Captor ArgumentCaptor<CreateResourceRequestV2> createResourceRequestV2ArgumentCaptor;

  @Test
  void checkAuthz_noThrow() throws InterruptedException, ApiException {
    // Setup mocks
    when(resourcesApi.resourcePermissionV2(RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION))
        .thenReturn(true);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    samService.checkAuthz(SAM_USER.getBearerToken(), RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION);
  }

  @Test
  void checkAuthz_throwsForbiddenException() throws ApiException {
    var token = SAM_USER.getBearerToken();
    // Setup mocks
    when(resourcesApi.resourcePermissionV2(RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION))
        .thenReturn(false);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    setupSamUserInfoMock(true);
    when(samClient.usersApi(anyString())).thenReturn(usersApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    Assertions.assertThrows(
        ForbiddenException.class,
        () -> samService.checkAuthz(token, RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION));
  }

  @Test
  void isAuthorized_success() throws ApiException, InterruptedException {
    // Setup mocks
    when(resourcesApi.resourcePermissionV2(RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION))
        .thenReturn(true);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    assertTrue(
        samService.isAuthorized(
            SAM_USER.getBearerToken(), RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION));
  }

  @Test
  void isAuthorized_noPermission() throws ApiException, InterruptedException {
    // Setup mocks
    when(resourcesApi.resourcePermissionV2(RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION))
        .thenReturn(false);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    assertFalse(
        samService.isAuthorized(
            SAM_USER.getBearerToken(), RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION));
  }

  @Test
  void isAuthorized_throwsSamInternalServerErrorException() throws ApiException {
    var token = SAM_USER.getBearerToken();
    // Setup mocks
    doThrow(API_EXCEPTION)
        .when(resourcesApi)
        .resourcePermissionV2(RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    Assertions.assertThrows(
        SamInternalServerErrorException.class,
        () -> samService.isAuthorized(token, RESOURCE_TYPE, RESOURCE_ID, RESOURCE_ACTION));
  }

  @Test
  void checkUserEnabled_noThrow() throws ApiException, InterruptedException {
    // Setup Mocks
    setupSamUserInfoMock(true);
    when(samClient.usersApi(anyString())).thenReturn(usersApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    samService.checkUserEnabled(SAM_USER.getBearerToken());
  }

  @Test
  void checkUserEnabled_throwsUnauthorizedException() throws ApiException {
    var token = SAM_USER.getBearerToken();
    // Setup Mocks
    setupSamUserInfoMock(false);
    when(samClient.usersApi(anyString())).thenReturn(usersApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    Assertions.assertThrows(UnauthorizedException.class, () -> samService.checkUserEnabled(token));
  }

  @Test
  void createLandingZone_success() throws ApiException, InterruptedException {
    var listOfUsers = List.of(SAM_USER.getEmail());
    Map<String, AccessPolicyMembershipRequest> policies = new HashMap<>();
    policies.put(
        "owner",
        new AccessPolicyMembershipRequest()
            .addMemberEmailsItem(SAM_USER.getEmail())
            .addRolesItem(SamConstants.SamRole.OWNER));
    policies.put(
        "user",
        new AccessPolicyMembershipRequest()
            .memberEmails(listOfUsers)
            .addRolesItem(SamConstants.SamRole.USER));
    // Setup Mocks
    setupSamUserInfoMock(true);
    when(samClient.getLandingZoneResourceUsers()).thenReturn(listOfUsers);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    when(samClient.usersApi(anyString())).thenReturn(usersApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    samService.createLandingZone(SAM_USER.getBearerToken(), BILLING_PROFILE_ID, LANDING_ZONE_ID);
    // Verify
    verifyCreateResourceV2(policies);
  }

  @Test
  void createLandingZone_noUsersInConfig() throws ApiException, InterruptedException {
    var listOfUsers = Collections.EMPTY_LIST;
    Map<String, AccessPolicyMembershipRequest> policies = new HashMap<>();
    policies.put(
        "owner",
        new AccessPolicyMembershipRequest()
            .addMemberEmailsItem(SAM_USER.getEmail())
            .addRolesItem(SamConstants.SamRole.OWNER));
    // Setup Mocks
    setupSamUserInfoMock(false);
    when(samClient.getLandingZoneResourceUsers()).thenReturn(listOfUsers);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    when(samClient.usersApi(anyString())).thenReturn(usersApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    samService.createLandingZone(SAM_USER.getBearerToken(), BILLING_PROFILE_ID, LANDING_ZONE_ID);
    // Verify
    verifyCreateResourceV2(policies);
  }

  @Test
  void createLandingZone_throwsSamInternalServerErrorException() throws ApiException {
    var listOfUsers = List.of(SAM_USER.getEmail());
    var token = SAM_USER.getBearerToken();
    // Setup Mocks
    setupSamUserInfoMock(false);
    when(samClient.usersApi(anyString())).thenReturn(usersApi);
    when(samClient.getLandingZoneResourceUsers()).thenReturn(listOfUsers);
    doThrow(API_EXCEPTION)
        .when(resourcesApi)
        .createResourceV2(
            eq(SamConstants.SamResourceType.LANDING_ZONE), any(CreateResourceRequestV2.class));
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    Assertions.assertThrows(
        SamInternalServerErrorException.class,
        () -> samService.createLandingZone(token, BILLING_PROFILE_ID, LANDING_ZONE_ID));
  }

  @Test
  void deleteLandingZone_Success() throws ApiException, InterruptedException {
    // Setup Mocks
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    samService.deleteLandingZone(SAM_USER.getBearerToken(), LANDING_ZONE_ID);
    // Verify
    verify(resourcesApi)
        .deleteResourceV2(SamConstants.SamResourceType.LANDING_ZONE, LANDING_ZONE_ID.toString());
  }

  @Test
  void deleteLandingZone_notFound() throws ApiException, InterruptedException {
    // Setup Mocks
    doThrow(new ApiException("...", HttpStatus.SC_NOT_FOUND, null, null))
        .when(resourcesApi)
        .deleteResourceV2(SamConstants.SamResourceType.LANDING_ZONE, LANDING_ZONE_ID.toString());
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    samService.deleteLandingZone(SAM_USER.getBearerToken(), LANDING_ZONE_ID);
    // Verify
    verify(resourcesApi)
        .deleteResourceV2(SamConstants.SamResourceType.LANDING_ZONE, LANDING_ZONE_ID.toString());
  }

  @Test
  void deleteLandingZone_throws() throws ApiException {
    var token = SAM_USER.getBearerToken();
    // Setup Mocks
    doThrow(API_EXCEPTION)
        .when(resourcesApi)
        .deleteResourceV2(SamConstants.SamResourceType.LANDING_ZONE, LANDING_ZONE_ID.toString());
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    Assertions.assertThrows(
        SamInternalServerErrorException.class,
        () -> samService.deleteLandingZone(token, LANDING_ZONE_ID));
  }

  @Test
  void listLandingZoneResourceIds_EmptyList_Success() throws InterruptedException, ApiException {
    // Setup Mocks
    UserResourcesResponse resourcesResponse = new UserResourcesResponse();
    resourcesResponse.resourceId("resourceId");
    RolesAndActions rolesAndActions = new RolesAndActions();
    rolesAndActions.addActionsItem("");
    resourcesResponse.setDirect(rolesAndActions);
    when(resourcesApi.listResourcesAndPoliciesV2(SamConstants.SamResourceType.LANDING_ZONE))
        .thenReturn(List.of(resourcesResponse));
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    var list = samService.listLandingZoneResourceIds(SAM_USER.getBearerToken());
    // Verify
    assertEquals(Collections.EMPTY_LIST, list);
  }

  @Test
  void listLandingZoneResourceIds_OneResource_Success() throws InterruptedException, ApiException {
    UserResourcesResponse resourcesResponse = new UserResourcesResponse();
    resourcesResponse.resourceId(LANDING_ZONE_ID.toString());
    RolesAndActions rolesAndActions = new RolesAndActions();
    rolesAndActions.addActionsItem(RESOURCE_ACTION);
    resourcesResponse.setDirect(rolesAndActions);
    // Setup Mocks
    when(resourcesApi.listResourcesAndPoliciesV2(SamConstants.SamResourceType.LANDING_ZONE))
        .thenReturn(List.of(resourcesResponse));
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    var list = samService.listLandingZoneResourceIds(SAM_USER.getBearerToken());
    // Verify
    assertEquals(List.of(LANDING_ZONE_ID), list);
  }

  @Test
  void listLandingZoneResourceIds_listOfResources_Success()
      throws InterruptedException, ApiException {
    UUID landingZoneIdInherited = UUID.randomUUID();
    UUID landingZoneIdPublic = UUID.randomUUID();
    // Direct
    UserResourcesResponse resourcesResponse = new UserResourcesResponse();
    resourcesResponse.resourceId(LANDING_ZONE_ID.toString());
    RolesAndActions rolesAndActions = new RolesAndActions();
    rolesAndActions.addActionsItem(RESOURCE_ACTION);
    rolesAndActions.addActionsItem("other_action");
    resourcesResponse.setDirect(rolesAndActions);
    // Inherited
    UserResourcesResponse resourcesResponseInherited = new UserResourcesResponse();
    resourcesResponseInherited.resourceId(landingZoneIdInherited.toString());
    RolesAndActions rolesAndActionsInherited = new RolesAndActions();
    rolesAndActionsInherited.addActionsItem("other_action");
    resourcesResponseInherited.setInherited(rolesAndActionsInherited);
    // Public
    UserResourcesResponse resourcesResponsePublic = new UserResourcesResponse();
    resourcesResponsePublic.resourceId(landingZoneIdPublic.toString());
    RolesAndActions rolesAndActionsPublic = new RolesAndActions();
    rolesAndActionsPublic.addActionsItem("some_action");
    resourcesResponsePublic.setPublic(rolesAndActionsPublic);
    // Setup Mocks
    when(resourcesApi.listResourcesAndPoliciesV2(SamConstants.SamResourceType.LANDING_ZONE))
        .thenReturn(
            List.of(resourcesResponse, resourcesResponseInherited, resourcesResponsePublic));
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    var list = samService.listLandingZoneResourceIds(SAM_USER.getBearerToken());
    // Verify
    assertEquals(List.of(LANDING_ZONE_ID, landingZoneIdInherited, landingZoneIdPublic), list);
  }

  @Test
  void listLandingZoneResourceIds_throws() throws ApiException {
    var token = SAM_USER.getBearerToken();
    // Setup Mocks
    doThrow(API_EXCEPTION)
        .when(resourcesApi)
        .listResourcesAndPoliciesV2(SamConstants.SamResourceType.LANDING_ZONE);
    when(samClient.resourcesApi(anyString())).thenReturn(resourcesApi);
    samService = new LandingZoneSamService(samClient);
    // Test
    Assertions.assertThrows(
        SamInternalServerErrorException.class, () -> samService.listLandingZoneResourceIds(token));
  }

  private void setupSamUserInfoMock(boolean enabled) throws ApiException {
    when(usersApi.getUserStatusInfo())
        .thenReturn(
            new UserStatusInfo()
                .userEmail(SAM_USER.getEmail())
                .userSubjectId(SAM_USER.getSubjectId())
                .enabled(enabled));
  }

  private void verifyCreateResourceV2(Map<String, AccessPolicyMembershipRequest> policies)
      throws ApiException {
    verify(resourcesApi)
        .createResourceV2(
            eq(SamConstants.SamResourceType.LANDING_ZONE),
            createResourceRequestV2ArgumentCaptor.capture());
    var createResourceRequest = createResourceRequestV2ArgumentCaptor.getValue();
    assertEquals(LANDING_ZONE_ID.toString(), createResourceRequest.getResourceId());
    assertEquals(
        new FullyQualifiedResourceId()
            .resourceId(BILLING_PROFILE_ID.toString())
            .resourceTypeName(SamConstants.SamResourceType.SPEND_PROFILE),
        createResourceRequest.getParent());
    assertEquals(Collections.EMPTY_LIST, createResourceRequest.getAuthDomain());
    assertEquals(policies, createResourceRequest.getPolicies());
  }
}
