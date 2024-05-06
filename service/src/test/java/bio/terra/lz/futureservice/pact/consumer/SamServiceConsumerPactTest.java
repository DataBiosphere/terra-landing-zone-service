package bio.terra.lz.futureservice.pact.consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.library.configuration.LandingZoneSamConfiguration;
import bio.terra.landingzone.service.iam.LandingZoneSamClient;
import bio.terra.landingzone.service.iam.LandingZoneSamService;
import bio.terra.lz.futureservice.app.service.status.SamStatusService;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("pact-test")
@ExtendWith(PactConsumerTestExt.class)
public class SamServiceConsumerPactTest {
  private static final String RESOURCE_TYPE = "resourceType";
  private static final String ACTION = "action";
  private static final String RESOURCE_ID = "5739d704-b0f7-11ee-9f52-00155df9466d";
  private static final String SAM_LISTRESOURCESANDPOLICIESV2_RESPONSE_BODY =
      """
          [
            {
              "resourceId": "%s"
            }
          ]
          """
          .formatted(RESOURCE_ID);

  @Pact(consumer = "terra-landing-zone-service", provider = "sam")
  public RequestResponsePact statusApiPact(PactDslWithProvider builder) {
    return builder
        .given("Sam is ok")
        .uponReceiving("a status request")
        .path("/status")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().booleanValue("ok", true))
        .toPact();
  }

  /*
    Tests are commented out because just marking them as disabled or with @PactIgnore wasn't working,
    and they were still being added to the contract.


    @Disabled
    @PactIgnore
    @Pact(consumer = "terra-landing-zone-service", provider = "sam")
    public RequestResponsePact resourceExistingPermissionV2Pact(PactDslWithProvider builder) {
      return builder
          .given("permission exists")
          .uponReceiving("a request to check permissions")
          .path("/api/resources/v2/%s/%s/action/%s".formatted(RESOURCE_TYPE, RESOURCE_ID, ACTION))
          .method("GET")
          .willRespondWith()
          .status(200)
          .body("true")
          .toPact();
    }
  */
  @Pact(consumer = "terra-landing-zone-service", provider = "sam")
  public RequestResponsePact userStatusInfoPact(PactDslWithProvider builder) {
    var responseBody =
        new PactDslJsonBody()
            .stringType("userSubjectId")
            .stringType("userEmail")
            .booleanType("enabled");
    return builder
        .given("user status info request with access token")
        .uponReceiving("a request for the user's status")
        .path("/register/user/v2/self/info")
        .method("GET")
        .headers("Authorization", "Bearer accessToken")
        .willRespondWith()
        .status(200)
        .body(responseBody)
        .toPact();
  }

  /*
  @Disabled
  @PactIgnore
  @Pact(consumer = "terra-landing-zone-service", provider = "sam")
  public RequestResponsePact listLandingZoneResourceIdsPact(PactDslWithProvider builder) {
    return builder
        .given("landing zone resource identifiers exist")
        .uponReceiving("a request to list landing zone resource ids")
        .path("/api/resources/v2/%s".formatted(SamConstants.SamResourceType.LANDING_ZONE))
        .method("GET")
        .willRespondWith()
        .body(SAM_LISTRESOURCESANDPOLICIESV2_RESPONSE_BODY)
        .toPact();
  }
  */

  @Test
  @PactTestFor(pactMethod = "statusApiPact", pactVersion = PactSpecVersion.V3)
  public void testSamServiceStatusCheck(MockServer mockServer) {
    LandingZoneSamConfiguration config = setupSamConfiguration(mockServer);
    var lzSamClient = new LandingZoneSamClient(config, Optional.empty());
    var samStatusService = new SamStatusService(lzSamClient);
    var system = samStatusService.status();

    assertTrue(system.isOk());
  }

  @Test
  @PactTestFor(pactMethod = "userStatusInfoPact", pactVersion = PactSpecVersion.V3)
  public void testSamServiceUserStatusInfo(MockServer mockServer) throws Exception {
    LandingZoneSamConfiguration config = setupSamConfiguration(mockServer);
    var lzSamClient = new LandingZoneSamClient(config, Optional.empty());
    var samService = new LandingZoneSamService(lzSamClient);
    samService.checkUserEnabled(new BearerToken("accessToken"));
  }

  /*
  @PactIgnore
  @Disabled
  @Test
  @PactTestFor(pactMethod = "resourceExistingPermissionV2Pact", pactVersion = PactSpecVersion.V3)
  public void testIsAuthorized(MockServer mockServer) throws InterruptedException {
    LandingZoneSamConfiguration config = setupSamConfiguration(mockServer);
    var lzSamClient = new LandingZoneSamClient(config, Optional.empty());
    var samService = new LandingZoneSamService(lzSamClient);

    assertTrue(
        samService.isAuthorized(
            new BearerToken("accessToken"), RESOURCE_TYPE, RESOURCE_ID, ACTION));
  }

  @PactIgnore
  @Disabled
  @Test
  @PactTestFor(pactMethod = "listLandingZoneResourceIdsPact", pactVersion = PactSpecVersion.V3)
  public void testlistLandingZoneResourceIds(MockServer mockServer) throws InterruptedException {
    LandingZoneSamConfiguration config = setupSamConfiguration(mockServer);
    var lzSamClient = new LandingZoneSamClient(config, Optional.empty());
    var samService = new LandingZoneSamService(lzSamClient);

    List<UUID> response = samService.listLandingZoneResourceIds(new BearerToken("accessToken"));
    assertEquals(1, response.size());
    assertEquals(RESOURCE_ID, response.get(0).toString());
  }
  */

  private static LandingZoneSamConfiguration setupSamConfiguration(MockServer mockServer) {
    LandingZoneSamConfiguration config = new LandingZoneSamConfiguration();
    config.setBasePath(mockServer.getUrl());
    config.setLandingZoneResourceUsers(Collections.singletonList("lzuser@test.com"));
    return config;
  }
}
