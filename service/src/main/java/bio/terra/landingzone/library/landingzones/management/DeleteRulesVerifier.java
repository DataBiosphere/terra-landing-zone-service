package bio.terra.landingzone.library.landingzones.management;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.deleterules.AKSAgentPoolHasMoreThanOneNode;
import bio.terra.landingzone.library.landingzones.management.deleterules.AzureRelayHasHybridConnections;
import bio.terra.landingzone.library.landingzones.management.deleterules.BatchAccountHasNodePools;
import bio.terra.landingzone.library.landingzones.management.deleterules.DeleteRule;
import bio.terra.landingzone.library.landingzones.management.deleterules.LandingZoneRuleDeleteException;
import bio.terra.landingzone.library.landingzones.management.deleterules.PostgreSQLServerHasDBs;
import bio.terra.landingzone.library.landingzones.management.deleterules.StorageAccountHasContainers;
import bio.terra.landingzone.library.landingzones.management.deleterules.VmsAreAttachedToVnet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/** Contains and applies a list of rules to a resource to confirm if it can be deleted. */
public class DeleteRulesVerifier {
  private final List<DeleteRule> deleteRules;

  public DeleteRulesVerifier(List<DeleteRule> deleteRules) {
    this.deleteRules = deleteRules;
  }

  public DeleteRulesVerifier(ArmManagers armManagers) {
    this(
        List.of(
            new AKSAgentPoolHasMoreThanOneNode(armManagers),
            new AzureRelayHasHybridConnections(armManagers),
            new BatchAccountHasNodePools(armManagers),
            new PostgreSQLServerHasDBs(armManagers),
            new StorageAccountHasContainers(armManagers),
            new VmsAreAttachedToVnet(armManagers)));
  }

  public void checkIfRulesAllowDelete(List<ResourceToDelete> landingZoneResources)
      throws LandingZoneRuleDeleteException {
    if (deleteRules == null) {
      return;
    }
    String rulesResultsMessage =
        landingZoneResources.stream()
            .flatMap(this::applyRulesToResource)
            .filter(r -> !r.isDeletable())
            .map(
                r ->
                    String.format(
                        "rule:%s reason:%s resourceType:%s",
                        r.ruleName(), r.reason(), r.resourceType()))
            .collect(Collectors.joining(", "));

    if (!StringUtils.isEmpty(rulesResultsMessage)) {
      String msg = String.format("Cannot delete the landing zone. %s", rulesResultsMessage);
      throw new LandingZoneRuleDeleteException(msg);
    }
  }

  public Stream<DeleteRuleResult> applyRulesToResource(ResourceToDelete resourceToDelete) {
    if (deleteRules == null || deleteRules.isEmpty()) {
      return Stream.empty();
    }
    return deleteRules.stream().map(r -> r.applyRule(resourceToDelete));
  }
}
