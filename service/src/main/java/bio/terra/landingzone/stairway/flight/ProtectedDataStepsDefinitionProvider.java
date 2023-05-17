package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import bio.terra.landingzone.stairway.flight.create.resource.step.ConnectLongTermLogStorageStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelRunPlaybookAutomationRule;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.FetchLongTermStorageAccountStep;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import com.azure.resourcemanager.AzureResourceManager;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectedDataStepsDefinitionProvider extends CromwellStepsDefinitionProvider {
  private static final Logger logger =
      LoggerFactory.getLogger(ProtectedDataStepsDefinitionProvider.class);

  @Override
  public List<Pair<Step, RetryRule>> get(
      ArmManagers lzArmManagers,
      AzureResourceManager adminSubResourceManager,
      ParametersResolver parametersResolver,
      ResourceNameGenerator resourceNameGenerator,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    // inherit all cromwell steps and define specific below
    var protectedDataSteps =
        new ArrayList<>(
            super.get(
                lzArmManagers,
                adminSubResourceManager,
                parametersResolver,
                resourceNameGenerator,
                landingZoneProtectedDataConfiguration));

    protectedDataSteps.add(
        Pair.of(
            new FetchLongTermStorageAccountStep(
                lzArmManagers,
                adminSubResourceManager,
                landingZoneProtectedDataConfiguration.getLongTermStorageResourceGroupName()),
            RetryRules.cloud()));

    protectedDataSteps.add(
        Pair.of(
            new ConnectLongTermLogStorageStep(
                lzArmManagers,
                parametersResolver,
                resourceNameGenerator,
                landingZoneProtectedDataConfiguration.getLongTermStorageTableNames()),
            RetryRules.cloud()));

    protectedDataSteps.add(
        Pair.of(
            new CreateSentinelStep(lzArmManagers, parametersResolver, resourceNameGenerator),
            RetryRules.cloud()));

    protectedDataSteps.add(
        Pair.of(
            new CreateSentinelRunPlaybookAutomationRule(
                lzArmManagers,
                parametersResolver,
                resourceNameGenerator,
                landingZoneProtectedDataConfiguration),
            RetryRules.cloud()));

    return protectedDataSteps;
  }
}
