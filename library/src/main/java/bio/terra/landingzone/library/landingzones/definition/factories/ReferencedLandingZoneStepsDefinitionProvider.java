package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.factories.validation.InputParametersValidationFactory;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.reference.resource.step.*;
import bio.terra.landingzone.stairway.flight.create.resource.step.*;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class ReferencedLandingZoneStepsDefinitionProvider implements StepsDefinitionProvider {
  private static final String LZ_NAME = "Referenced Landing Zone";
  private static final String LZ_DESC =
      "Referenced landing zones utilize existing resources. These resources should be available and deployed in the managed resource group. Expected resources include: VNet, AKS, Batch Account,"
          + " Storage Account, PostgreSQL server, Subnets for AKS, Batch, Postgresql, and Compute";

  @Override
  public String landingZoneDefinition() {
    return StepsDefinitionFactoryType.REFERENCED_DEFINITION_STEPS_PROVIDER_TYPE.getValue();
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
                    StepsDefinitionFactoryType.REFERENCED_DEFINITION_STEPS_PROVIDER_TYPE)),
            RetryRules.shortExponential()),
        Pair.of(new ReferencedVnetStep(armManagers), RetryRules.cloud()),
        Pair.of(new ReferencedAksStep(armManagers), RetryRules.cloud()),
        Pair.of(new ReferencedBatchStep(armManagers), RetryRules.cloud()),
        Pair.of(new ReferencedStorageStep(armManagers), RetryRules.cloud()),
        Pair.of(new ReferencedRelayNamespaceStep(armManagers), RetryRules.cloud()),
        Pair.of(new ReferencedAppInsightsStep(armManagers), RetryRules.cloud()));
  }
}
