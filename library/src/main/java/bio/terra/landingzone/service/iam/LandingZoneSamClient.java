package bio.terra.landingzone.service.iam;

import bio.terra.common.tracing.OkHttpClientTracingInterceptor;
import bio.terra.landingzone.library.configuration.LandingZoneSamConfiguration;
import io.opencensus.trace.Tracing;
import java.util.List;
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

  public LandingZoneSamClient(LandingZoneSamConfiguration samConfig) {
    this.samConfig = samConfig;
    this.okHttpClient = new ApiClient().getHttpClient();
  }

  private ApiClient getApiClient(String accessToken) {
    ApiClient apiClient = getApiClient();
    apiClient.setAccessToken(accessToken);
    return apiClient;
  }

  private ApiClient getApiClient() {
    var okHttpClientWithTracing =
        this.okHttpClient
            .newBuilder()
            .addInterceptor(new OkHttpClientTracingInterceptor(Tracing.getTracer()))
            .build();
    return new ApiClient()
        .setHttpClient(okHttpClientWithTracing)
        .setBasePath(samConfig.getBasePath());
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

  StatusApi statusApi() {
    return new StatusApi(getApiClient());
  }
}
