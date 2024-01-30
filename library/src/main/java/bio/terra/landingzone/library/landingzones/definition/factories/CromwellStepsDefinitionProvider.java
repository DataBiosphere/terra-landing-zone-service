package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.common.k8s.configmap.reader.AksConfigMapFileReaderImpl;
import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.InputParametersValidationFactory;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksCostOptimizationDataCollectionRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAppInsightsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateBatchNetworkSecurityGroupStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneFederatedIdentityStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLandingZoneIdentityStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsDataCollectionRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateLogAnalyticsWorkspaceStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateNetworkSecurityGroupStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresqlDNSStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreatePostgresqlDbStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateRelayNamespaceStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountCorsRules;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAccountStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateStorageAuditLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateVirtualNetworkLinkStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateVnetStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.EnableAksContainerInsightsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.EnableAksContainerLogV2Step;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetManagedResourceGroupInfo;
import bio.terra.landingzone.stairway.flight.create.resource.step.GetParametersResolver;
import bio.terra.landingzone.stairway.flight.create.resource.step.KubernetesClientProviderImpl;
import bio.terra.landingzone.stairway.flight.create.resource.step.ValidateLandingZoneParametersStep;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class CromwellStepsDefinitionProvider implements StepsDefinitionProvider {
  private static final String LZ_NAME = "Cromwell Landing Zone Base Resources";
  private static final String LZ_DESC =
      "Cromwell Base Resources: VNet, AKS Account & Nodepool, Batch Account,"
          + " Storage Account, PostgreSQL server, Subnets for AKS, Batch, Posgres, and Compute";

  @Override
  public String landingZoneDefinition() {
    return StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE.getValue();
  }

  @Override
  public DefinitionHeader header() {
    return new DefinitionHeader(LZ_NAME, LZ_DESC);
  }

  @Override
  public List<DefinitionVersion> availableVersions() {
    return List.of(DefinitionVersion.V1);
  }

  @Override
  public List<Pair<Step, RetryRule>> get(
      ArmManagers armManagers,
      ParametersResolverProvider parametersResolverProvider,
      ResourceNameProvider resourceNameProvider,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    return List.of(
        Pair.of(new GetManagedResourceGroupInfo(armManagers), RetryRules.cloud()),
        Pair.of(new GetParametersResolver(parametersResolverProvider), RetryRules.shortDatabase()),
        Pair.of(
            new ValidateLandingZoneParametersStep(
                InputParametersValidationFactory.buildValidators(
                    StepsDefinitionFactoryType.CROMWELL_BASE_DEFINITION_STEPS_PROVIDER_TYPE)),
            RetryRules.shortExponential()),
        Pair.of(
            new CreateNetworkSecurityGroupStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateBatchNetworkSecurityGroupStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(new CreateVnetStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreateLogAnalyticsWorkspaceStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(new CreatePostgresqlDNSStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreateVirtualNetworkLinkStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateLandingZoneIdentityStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(new CreatePostgresqlDbStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreateStorageAccountStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(new CreateBatchAccountStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreateStorageAccountCorsRules(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateLogAnalyticsDataCollectionRulesStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(new CreateAksStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreateLandingZoneFederatedIdentityStep(
                armManagers, new KubernetesClientProviderImpl()),
            RetryRules.cloud()),
        Pair.of(
            new CreateRelayNamespaceStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreateStorageAuditLogSettingsStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new CreateBatchLogSettingsStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreatePostgresLogSettingsStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(new CreateAppInsightsStep(armManagers, resourceNameProvider), RetryRules.cloud()),
        Pair.of(
            new CreateAksCostOptimizationDataCollectionRulesStep(armManagers, resourceNameProvider),
            RetryRules.cloud()),
        Pair.of(
            new EnableAksContainerLogV2Step(
                armManagers,
                new KubernetesClientProviderImpl(),
                new AksConfigMapFileReaderImpl(EnableAksContainerLogV2Step.CONFIG_MAP_PATH)),
            RetryRules.cloud()),
        Pair.of(new EnableAksContainerInsightsStep(armManagers), RetryRules.cloud()));
  }
}
