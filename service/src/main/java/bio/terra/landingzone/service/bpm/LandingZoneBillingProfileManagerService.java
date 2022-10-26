package bio.terra.landingzone.service.bpm;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.library.configuration.LandingZoneBillingProfileManagerConfiguration;
import bio.terra.landingzone.service.bpm.exception.BillingProfileNotFoundException;
import bio.terra.profile.api.ProfileApi;
import bio.terra.profile.client.ApiClient;
import bio.terra.profile.client.ApiException;
import bio.terra.profile.model.ProfileModel;
import bio.terra.profile.model.ProfileModelList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneBillingProfileManagerService {
  private static final Logger logger =
      LoggerFactory.getLogger(LandingZoneBillingProfileManagerService.class);
  private final LandingZoneBillingProfileManagerConfiguration bpmConfig;

  @Autowired
  public LandingZoneBillingProfileManagerService(
      LandingZoneBillingProfileManagerConfiguration bpmConfig) {
    this.bpmConfig = bpmConfig;
  }

  private ApiClient getApiClient(String accessToken) {
    ApiClient apiClient = new ApiClient().setBasePath(bpmConfig.getBasePath());
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }

  private ProfileApi profileApi(String accessToken) {
    return new ProfileApi(getApiClient(accessToken));
  }

  /**
   * Looks up a billing profile from BPM by id.
   *
   * @param bearerToken the bearer token of the caller
   * @param billingProfileId the billing profile ID
   * @return ProfileModel object
   */
  public ProfileModel getBillingProfile(BearerToken bearerToken, UUID billingProfileId) {
    try {
      return profileApi(bearerToken.getToken()).getProfile(billingProfileId);
    } catch (ApiException e) {
      throw new BillingProfileNotFoundException(
          String.format("Billing profile %s not found", billingProfileId.toString()), e);
    }
  }

  /**
   * Looks up all billing profiles from BPM that user has read access to.
   *
   * @param bearerToken the bearer token of the caller
   * @return ProfileModelList object
   */
  public ProfileModelList getBillingProfiles(BearerToken bearerToken) {
    try {
      return profileApi(bearerToken.getToken()).listProfiles(0, 0);
    } catch (ApiException e) {
      throw new BillingProfileNotFoundException("No billing profiles found", e);
    }
  }

  public void verifyUserAccess(BearerToken bearerToken, UUID billingProfileId) {
    // Call BPM to get billing profile for a user.
    var profile = getBillingProfile(bearerToken, billingProfileId);
  }
}
