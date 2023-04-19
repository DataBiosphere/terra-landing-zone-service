package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneAzureConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAppInsightsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsDataCollectionRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsWorkspaceStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresqlDbStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePrivateEndpointStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateRelayStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountCorsRules;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAuditLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateVnetStep;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class CromwellStepsDefinitionProvider implements StepsDefinitionProvider {
  @Override
  public List<Pair<Step, RetryRule>> get(
      LandingZoneAzureConfiguration landingZoneAzureConfiguration,
      ResourceNameGenerator resourceNameGenerator) {
    /*
     * ~ - depends on
     * 1) VNet step
     * 2) Log analytics step
     * 3) Postgres
     * 4) Storage account
     * 5) Batch account
     *
     * 6) Cors rules ~  4)
     * 7) Data collection rules ~ 3)
     * 8) Private endpoint ~ 1), 2)
     * 9) AKS ~ 1)
     * 10) Relay
     * 11) Storage audit log settings ~ 3), 4)
     * 12) Batch log settings ~ 3), 5)
     * 13) Postgres log settings ~ 2), 3)
     * 14) AppInsights ~ 3)
     * */
    return List.of(
        Pair.of(
            new CreateVnetStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateLogAnalyticsWorkspaceStep(
                landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreatePostgresqlDbStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateStorageAccountStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateBatchAccountStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateStorageAccountCorsRules(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateLogAnalyticsDataCollectionRulesStep(
                landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreatePrivateEndpointStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateAksStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateRelayStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateStorageAuditLogSettingsStep(
                landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateBatchLogSettingsStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreatePostgresLogSettingsStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()),
        Pair.of(
            new CreateAppInsightsStep(landingZoneAzureConfiguration, resourceNameGenerator),
            RetryRules.cloud()));
  }
}
