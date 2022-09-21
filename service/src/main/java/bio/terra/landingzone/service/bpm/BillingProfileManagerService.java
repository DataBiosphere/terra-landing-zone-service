package bio.terra.landingzone.service.bpm;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.library.configuration.BillingProfileManagerConfiguration;
import bio.terra.landingzone.service.bpm.exception.BillingProfileNotFoundException;
import bio.terra.profile.api.ProfileApi;
import bio.terra.profile.client.ApiClient;
import bio.terra.profile.client.ApiException;
import bio.terra.profile.model.ProfileModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BillingProfileManagerService {
    private static final Logger logger = LoggerFactory.getLogger(BillingProfileManagerService.class);
    private final BillingProfileManagerConfiguration bpmConfig;

    @Autowired
    public BillingProfileManagerService(BillingProfileManagerConfiguration bpmConfig) {
        this.bpmConfig = bpmConfig;
    }

    private ApiClient getApiClient(String accessToken) {
        // OkHttpClient objects manage their own thread pools, so it's much more performant to share one
        // across requests.
        ApiClient apiClient =
                new ApiClient().setBasePath(bpmConfig.getBasePath());
        apiClient.setAccessToken(accessToken);
        return apiClient;
    }

    private ProfileApi profileApi(String accessToken) {
        return new ProfileApi(getApiClient(accessToken));
    }

    public ProfileModel getBillingProfile(BearerToken bearerToken, UUID billingProfileId) {
        try {
            return profileApi(bearerToken.getToken()).getProfile(billingProfileId);
        } catch (ApiException e) {
            throw new BillingProfileNotFoundException(String.format("Billing profile %s not found", billingProfileId.toString()), e);
        }
    }

}
