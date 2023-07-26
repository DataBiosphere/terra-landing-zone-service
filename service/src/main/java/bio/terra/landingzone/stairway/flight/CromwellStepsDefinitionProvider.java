package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.InputParametersValidationFactory;
import bio.terra.landingzone.stairway.flight.create.resource.step.AksConfigMapFileReaderImpl;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAppInsightsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneFederatedIdentityStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneIdentityStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsDataCollectionRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsWorkspaceStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresqlDNSStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresqlDbStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateRelayNamespaceStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountCorsRules;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAuditLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateVirtualNetworkLinkStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateVnetStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.EnableAksContainerLogV2Step;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.landingzone.stairway.flight.create.resource.step.KubernetesClientProviderImpl;
import bio.terra.landingzone.stairway.flight.create.resource.step.ValidateLandingZoneParametersStep;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class CromwellStepsDefinitionProvider implements StepsDefinitionProvider {
  // TODO: this doesn't take into account versioning
  @Override
  public List<Pair<Step, RetryRule>> get(
      ArmManagers armManagers,
      ParametersResolver parametersResolver,
      ResourceNameProvider resourceNameProvider,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    return List.of(
        Pair.of(
            new ValidateLandingZoneParametersStep(
                InputParametersValidationFactory.buildValidators(
                    StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE),
                parametersResolver),
            RetryRules.shortExponential()),
        Pair.of(new GetManagedResourceGroupInfo(armManagers), RetryRules.cloud()),
        Pair.of(
            new CreateVnetStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateLogAnalyticsWorkspaceStep(
                armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreatePostgresqlDNSStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateVirtualNetworkLinkStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateLandingZoneIdentityStep(
                armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreatePostgresqlDbStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateStorageAccountStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateBatchAccountStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateStorageAccountCorsRules(
                armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateLogAnalyticsDataCollectionRulesStep(
                armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateAksStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateLandingZoneFederatedIdentityStep(
                armManagers, new KubernetesClientProviderImpl()),
            RetryRules.cloud()),
        Pair.of(
            new CreateRelayNamespaceStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateStorageAuditLogSettingsStep(
                armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateBatchLogSettingsStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreatePostgresLogSettingsStep(
                armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateAppInsightsStep(armManagers, parametersResolver, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new EnableAksContainerLogV2Step(
                armManagers,
                new KubernetesClientProviderImpl(),
                new AksConfigMapFileReaderImpl(EnableAksContainerLogV2Step.CONFIG_MAP_PATH)),
            RetryRules.cloud()));
  }
}
