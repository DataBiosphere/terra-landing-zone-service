package bio.terra.landingzone.service.iam;

import bio.terra.common.tracing.OkHttpClientTracingInterceptor;
import bio.terra.landingzone.library.configuration.LandingZoneSamConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import java.util.List;
import java.util.Optional;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.api.ResourcesApi;
import org.broadinstitute.dsde.workbench.client.sam.api.StatusApi;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.springframework.stereotype.Component;

@Component
public class LandingZoneSamClient {
  private final LandingZoneSamConfiguration samConfig;
  private final OkHttpClient okHttpClient;

  public LandingZoneSamClient(
      LandingZoneSamConfiguration samConfig, Optional<OpenTelemetry> openTelemetry) {
    this.samConfig = samConfig;
    this.okHttpClient =
        openTelemetry
            .map(
                otel ->
                    new ApiClient()
                        .getHttpClient()
                        .newBuilder()
                        .addInterceptor(new OkHttpClientTracingInterceptor(otel))
                        .build())
            .orElse(new ApiClient().getHttpClient());
  }

  private ApiClient getApiClient(String accessToken) {
    ApiClient apiClient = getApiClient();
    apiClient.setBearerToken(accessToken);
    return apiClient;
  }

  private ApiClient getApiClient() {
    return new ApiClient().setHttpClient(this.okHttpClient).setBasePath(samConfig.getBasePath());
  }

  UsersApi usersApi(String accessToken) {
    return new UsersApi(getApiClient(accessToken));
  }

  ResourcesApi resourcesApi(String accessToken) {
    return new ResourcesApi(getApiClient(accessToken));
  }

  List<String> getLandingZoneResourceUsers() {
    return samConfig.getLandingZoneResourceUsers();
  }

  // expose to use inside service:SamStatusService class
  public StatusApi statusApi() {
    return new StatusApi(getApiClient());
  }
}
