package bio.terra.landingzone.stairway.flight.create.resource.step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.exception.MissingRequiredFieldsException;
import bio.terra.landingzone.stairway.flight.utils.AlertRulesHelper;
import bio.terra.profile.model.ProfileModel;
import bio.terra.stairway.StepResult;
import com.azure.resourcemanager.securityinsights.fluent.models.AlertRuleInner;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CreateSentinelAlertRulesStepTest extends BaseStepTest {

  @Mock LandingZoneProtectedDataConfiguration mockLandingZoneProtectedDataConfiguration;
  @Mock AlertRulesHelper mockAlertRuleAdapter;

  @Test
  void doStepSuccess() throws InterruptedException {
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of(
            GetManagedResourceGroupInfo.TARGET_MRG_KEY,
            ResourceStepFixture.createDefaultMrg(),
            CreateLogAnalyticsWorkspaceStep.LOG_ANALYTICS_RESOURCE_KEY,
            buildLandingZoneResource()));
    var mockRule = mock(AlertRuleInner.class);
    when(mockAlertRuleAdapter.buildScheduledAlertRuleFromTemplate(
            anyString(), anyString(), anyString()))
        .thenReturn(mockRule);

    var createSentinelAlertRulesStep =
        new CreateSentinelAlertRulesStep(
            mockArmManagers,
            mockParametersResolver,
            mockResourceNameGenerator,
            mockAlertRuleAdapter);

    var result = createSentinelAlertRulesStep.doStep(mockFlightContext);

    assertThat(result, equalTo(StepResult.getStepResultSuccess()));
    AlertRuleTemplates.getSentinelScheduledAlertRuleTemplateIds()
        .forEach(
            u ->
                verify(mockAlertRuleAdapter, times(1))
                    .createAlertRule(any(), eq(u), anyString(), anyString()));
    AlertRuleTemplates.getSentinelMlRuleTemplateIds()
        .forEach(
            u ->
                verify(mockAlertRuleAdapter, times(1))
                    .createAlertRule(any(), eq(u), anyString(), anyString()));
    AlertRuleTemplates.getSentinelNrtRuleTemplateIds()
        .forEach(
            u ->
                verify(mockAlertRuleAdapter, times(1))
                    .createAlertRule(any(), eq(u), anyString(), anyString()));
  }

  @Test
  void doStep_missingParamThrows() {
    setupFlightContext(
        mockFlightContext,
        Map.of(
            LandingZoneFlightMapKeys.BILLING_PROFILE,
            new ProfileModel().id(UUID.randomUUID()),
            LandingZoneFlightMapKeys.LANDING_ZONE_ID,
            LANDING_ZONE_ID),
        Map.of());

    var createSentinelAlertRulesStep =
        new CreateSentinelAlertRulesStep(
            mockArmManagers,
            mockParametersResolver,
            mockResourceNameGenerator,
            mockAlertRuleAdapter);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> createSentinelAlertRulesStep.doStep(mockFlightContext));
  }
}
