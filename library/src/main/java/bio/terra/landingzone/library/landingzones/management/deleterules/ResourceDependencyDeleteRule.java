package bio.terra.landingzone.library.landingzones.management.deleterules;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.management.DeleteRuleResult;
import bio.terra.landingzone.library.landingzones.management.ResourceToDelete;
import com.azure.core.util.logging.ClientLogger;

/**
 * A base rule that facilitates the implementation of rules that check for arm or functional
 * dependencies. A functional dependency is when a resource has a state that indicates usage by a
 * workspace resource. For example, an Azure Relay namespace can't be deleted because it has one or
 * more hybrid connection.
 */
public abstract class ResourceDependencyDeleteRule implements DeleteRule {
  protected final ArmManagers armManagers;

  protected static final ClientLogger logger = new ClientLogger(ResourceDependencyDeleteRule.class);

  protected ResourceDependencyDeleteRule(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }

  public abstract String getExpectedType();

  public abstract boolean hasDependentResources(ResourceToDelete resourceToDelete);

  @Override
  public DeleteRuleResult applyRule(ResourceToDelete genericResource) {
    if (!genericResource.resource().type().equalsIgnoreCase(getExpectedType())) {
      return new DeleteRuleResult(
          true,
          getClass().getSimpleName(),
          "Rule is not applicable to this azure resource type",
          genericResource.resource().type());
    }

    return applyDependencyRule(genericResource);
  }

  private DeleteRuleResult applyDependencyRule(ResourceToDelete resourceToDelete) {
    logger.info(
        "Applying delete rule. name:{} to resource:{}",
        getClass().getSimpleName(),
        resourceToDelete.resource().id());
    if (hasDependentResources(resourceToDelete)) {
      logger.info(
          "Resource has a dependant resource. resource:{}", resourceToDelete.resource().id());
      return new DeleteRuleResult(
          false,
          getClass().getSimpleName(),
          "The resource has at least one dependency and can't be deleted.",
          resourceToDelete.resource().type());
    }
    logger.info("Resource has no dependency. Resource:{}", resourceToDelete.resource().id());
    return new DeleteRuleResult(
        true,
        getClass().getSimpleName(),
        "The resource has no dependencies.",
        resourceToDelete.resource().type());
  }
}
