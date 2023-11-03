package bio.terra.landingzone.library.landingzones.management.deleterules;

import bio.terra.landingzone.library.landingzones.management.DeleteRuleResult;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;

/**
 * Interface of a delete rule. Implementations are applied before the resource is deleted to
 * determine if it can be deleted.
 */
public interface DeleteRule {
  DeleteRuleResult applyRule(ResourceToDelete genericResource);
}
