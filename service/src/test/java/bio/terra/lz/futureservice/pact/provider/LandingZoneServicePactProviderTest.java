package bio.terra.lz.futureservice.pact.provider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.BearerTokenFactory;
import bio.terra.landingzone.job.LandingZoneJobService;
import bio.terra.lz.futureservice.app.LandingZoneApplication;
import bio.terra.lz.futureservice.app.service.LandingZoneAppService;
import bio.terra.lz.futureservice.common.BaseSpringUnitTest;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZone;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDefinitionList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDeployedResource;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneDetails;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesList;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResourcesPurposeGroup;
import bio.terra.lz.futureservice.generated.model.ApiAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiCreateLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneJobResult;
import bio.terra.lz.futureservice.generated.model.ApiDeleteAzureLandingZoneResult;
import bio.terra.lz.futureservice.generated.model.ApiErrorReport;
import bio.terra.lz.futureservice.generated.model.ApiJobReport;
import bio.terra.lz.futureservice.generated.model.ApiResourceQuota;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * To run these tests against a local set of pacts, comment out the @PactBroker annotation and
 * replace with an @PactFolder annotation with a path to your pacts
 */
@AutoConfigureMockMvc
@Tag("pact-provider-test")
@Provider("terra-landing-zone-service")
@PactBroker
// @PactFolder("/path/to/pacts")
@SpringBootTest(
    properties = {
      "otel.instrumentation.spring-webmvc.enabled=false",
      "landingzone.landingzone-database.initialize-on-start=false",
      "landingzone.landingzone-database.upgrade-on-start=false",
    },
    classes = LandingZoneApplication.class)
public class LandingZoneServicePactProviderTest extends BaseSpringUnitTest {
  private static final String CONSUMER_BRANCH = System.getenv("CONSUMER_BRANCH");

  @MockBean BearerTokenFactory bearerTokenFactory;
  @MockBean LandingZoneAppService landingZoneAppService;
  @MockBean LandingZoneJobService landingZoneJobService;
  @Autowired private MockMvc mockMvc;

  @PactBrokerConsumerVersionSelectors
  public static SelectorBuilder consumerVersionSelectors() {
    // The following match condition basically says
    // If verification is triggered by Pact Broker webhook due to consumer pact change, verify only
    // the changed pact.
    // Otherwise, this is a PR, verify all consumer pacts in Pact Broker marked with a deployment
    // tag (e.g. dev, alpha).
    if (StringUtils.isBlank(CONSUMER_BRANCH)) {
      return new SelectorBuilder().mainBranch().deployedOrReleased();
    } else {
      return new SelectorBuilder().branch(CONSUMER_BRANCH);
    }
  }

  @BeforeEach
  void setUp(PactVerificationContext context) {
    context.setTarget(new MockMvcTestTarget(mockMvc));
    when(landingZoneAppService.deleteLandingZone(any(), any(), any(), any()))
        .thenReturn(
            new ApiDeleteAzureLandingZoneResult()
                .landingZoneId(UUID.randomUUID())
                .jobReport(
                    new ApiJobReport()
                        .id("random")
                        .description("fakedescription")
                        .statusCode(200)
                        .submitted("2024-05-28T14:29:00")
                        .resultURL("fake")
                        .status(ApiJobReport.StatusEnum.RUNNING)));
    when(landingZoneAppService.listAzureLandingZoneResources(any(), any()))
        .thenReturn(
            new ApiAzureLandingZoneResourcesList()
                .id(UUID.randomUUID())
                .resources(
                    List.of(
                        new ApiAzureLandingZoneResourcesPurposeGroup()
                            .purpose("testing")
                            .deployedResources(
                                List.of(
                                    new ApiAzureLandingZoneDeployedResource()
                                        .tags(Map.of("key", "value"))
                                        .resourceType("resourceType")
                                        .region("eastus")
                                        .resourceId("resourceIdTesting")
                                        .resourceName("fakeName"))))));

    when(landingZoneAppService.getResourceQuota(any(), any(), any()))
        .thenReturn(
            new ApiResourceQuota()
                .azureResourceId("fake")
                .landingZoneId(UUID.randomUUID())
                .resourceType("faketype")
                .putQuotaValuesItem("fake", "1"));

    when(landingZoneAppService.listAzureLandingZonesDefinitions(any()))
        .thenReturn(new ApiAzureLandingZoneDefinitionList());
    when(landingZoneAppService.listAzureLandingZones(any(), any()))
        .thenReturn(
            new ApiAzureLandingZoneList()
                .landingzones(
                    List.of(
                        new ApiAzureLandingZone()
                            .landingZoneId(UUID.randomUUID())
                            .billingProfileId(UUID.randomUUID())
                            .version("v1")
                            .definition("definition")
                            .region("eastus")
                            .createdDate(Instant.now().atOffset(ZoneOffset.UTC)))));
    when(landingZoneAppService.createAzureLandingZone(any(), any(), any()))
        .thenReturn(
            new ApiCreateLandingZoneResult()
                .definition("fake")
                .landingZoneId(UUID.randomUUID())
                .version("v1")
                .jobReport(
                    new ApiJobReport()
                        .status(ApiJobReport.StatusEnum.RUNNING)
                        .description("fake")
                        .id("fake")
                        .resultURL("fake")
                        .statusCode(200)
                        .submitted("2024-05-23T12:00:00")));
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void verifyPact(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @State("an unauthorized user")
  void unauthed(Map pactState) {
    when(landingZoneAppService.getCreateAzureLandingZoneResult(any(), any()))
        .thenThrow(new UnauthorizedException("unauthorized"));
    when(landingZoneAppService.getDeleteAzureLandingZoneResult(any(), any(), any()))
        .thenThrow(new UnauthorizedException("unauthorized"));
  }

  @State("An existing landing zone creation job")
  Map<String, Object> existingLandingZoneCreationJobState(Map<?, ?> pactState) {
    assertThat(pactState, hasEntry(is("asyncJobId"), notNullValue()));
    var creationJobId = pactState.get("asyncJobId");

    when(landingZoneAppService.getCreateAzureLandingZoneResult(any(), any()))
        .thenReturn(
            new ApiAzureLandingZoneResult()
                .landingZone(
                    new ApiAzureLandingZoneDetails()
                        .id(UUID.randomUUID())
                        .resources(
                            List.of(
                                new ApiAzureLandingZoneDeployedResource()
                                    .resourceName("fake")
                                    .region("eastus")
                                    .resourceId("fakeid")
                                    .resourceType("faketype")
                                    .tags(Map.of()))))
                .jobReport(
                    new ApiJobReport()
                        .status(ApiJobReport.StatusEnum.SUCCEEDED)
                        .description("fake")
                        .id("fake")
                        .resultURL("fake")
                        .statusCode(200)
                        .completed("2024-05-23T12:00:00")
                        .submitted("2024-05-23T12:00:00")));
    return Map.of("asyncJobId", creationJobId);
  }

  @State("An existing landing zone deletion job")
  Map<String, Object> existingLandingZoneDeletionJob(Map<?, ?> pactState) {
    assertThat(pactState, hasEntry(is("asyncJobId"), notNullValue()));
    var deletionJobId = pactState.get("asyncJobId");

    assertThat(pactState, hasEntry(is("jobState"), notNullValue()));
    var jobState = pactState.get("jobState").toString();

    var result =
        new ApiDeleteAzureLandingZoneJobResult()
            .landingZoneId(UUID.randomUUID())
            .errorReport(new ApiErrorReport().message("fake").statusCode(500))
            .jobReport(
                new ApiJobReport()
                    .status(ApiJobReport.StatusEnum.fromValue(jobState))
                    .description("fake")
                    .id(deletionJobId.toString())
                    .resultURL("fake")
                    .statusCode(200)
                    .completed("2024-05-23T12:00:00")
                    .submitted("2024-05-23T12:00:00"));

    if (jobState.equals(ApiJobReport.StatusEnum.FAILED.toString())) {
      result.setErrorReport(new ApiErrorReport().message("fake").statusCode(500));
    }
    when(landingZoneAppService.getDeleteAzureLandingZoneResult(any(), any(), any()))
        .thenReturn(result);

    return Map.of("asyncJobId", deletionJobId, "landingZoneId", UUID.randomUUID().toString());
  }

  @State("An existing landing zone linked to a billing profile")
  Map<String, Object> existingLandingZoneLinkedToBillingProfile(Map<?, ?> pactState) {
    when(landingZoneAppService.getAzureLandingZone(any(), any()))
        .thenReturn(
            new ApiAzureLandingZone()
                .billingProfileId(UUID.randomUUID())
                .landingZoneId(UUID.randomUUID())
                .createdDate(OffsetDateTime.now())
                .region("eastus")
                .version("v1")
                .definition("fake"));
    return Map.of("billingProfileId", UUID.randomUUID().toString());
  }

  @State("An existing landing zone")
  Map<String, Object> existingLandingZone() {
    when(landingZoneAppService.getAzureLandingZone(any(), any()))
        .thenReturn(
            new ApiAzureLandingZone()
                .billingProfileId(UUID.randomUUID())
                .landingZoneId(UUID.randomUUID())
                .createdDate(OffsetDateTime.now())
                .region("eastus")
                .version("v1")
                .definition("fake"));
    return Map.of("landingZoneId", UUID.randomUUID().toString());
  }
}
