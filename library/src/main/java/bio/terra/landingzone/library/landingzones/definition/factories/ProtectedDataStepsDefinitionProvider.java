package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.common.utils.RetryRules;
import bio.terra.landingzone.library.configuration.LandingZoneProtectedDataConfiguration;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.stairway.flight.ParametersResolverProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.create.resource.step.ConnectLongTermLogStorageStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateAksLogSettingsStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelAlertRulesStep;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelRunPlaybookAutomationRule;
import bio.terra.landingzone.stairway.flight.create.resource.step.CreateSentinelStep;
import bio.terra.landingzone.stairway.flight.utils.AlertRulesHelper;
import bio.terra.landingzone.stairway.flight.utils.ProtectedDataAzureStorageHelper;
import bio.terra.stairway.RetryRule;
import bio.terra.stairway.Step;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class ProtectedDataStepsDefinitionProvider extends CromwellStepsDefinitionProvider {
  private static final String LZ_NAME = "Protected data Landing Zone Base Resources";
  private static final String LZ_DESC =
      "Protected data landing zones are intended to fulfill legal requirements for data governed by a compliance standard, "
          + "such as HIPAA protected data, federal controlled-access data, etc. They deploy additional resources to the landing "
          + "zone for additional security monitoring (using Azure Sentinel) and exporting logs to centralized long-term storage for retention.";

  @Override
  public String landingZoneDefinition() {
    return StepsDefinitionFactoryType.PROTECTED_DATA_DEFINITION_STEPS_PROVIDER_NAME.getValue();
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
      ParametersResolverProvider parametersResolverProvider,
      ResourceNameProvider resourceNameProvider,
      LandingZoneProtectedDataConfiguration landingZoneProtectedDataConfiguration) {
    // inherit all cromwell steps and define specific below
    var protectedDataSteps =
        new ArrayList<>(
            super.get(
                parametersResolverProvider,
                resourceNameProvider,
                landingZoneProtectedDataConfiguration));

    protectedDataSteps.add(
        Pair.of(
            new ConnectLongTermLogStorageStep(
                resourceNameProvider,
                new ProtectedDataAzureStorageHelper(),
                landingZoneProtectedDataConfiguration.getLongTermStorageTableNames(),
                landingZoneProtectedDataConfiguration.getLongTermStorageAccountIds()),
            RetryRules.cloud()));

    protectedDataSteps.add(
        Pair.of(new CreateSentinelStep(resourceNameProvider), RetryRules.cloud()));

    protectedDataSteps.add(
        Pair.of(
            new CreateSentinelRunPlaybookAutomationRule(
                resourceNameProvider, landingZoneProtectedDataConfiguration),
            RetryRules.cloud()));

    protectedDataSteps.add(
        Pair.of(
            new CreateSentinelAlertRulesStep(
                resourceNameProvider,
                new AlertRulesHelper(),
                landingZoneProtectedDataConfiguration),
            RetryRules.cloudLongRunning()));
    protectedDataSteps.add(
        Pair.of(
            new CreateAksLogSettingsStep(
                resourceNameProvider, landingZoneProtectedDataConfiguration),
            RetryRules.cloud()));

    return protectedDataSteps;
  }
}
