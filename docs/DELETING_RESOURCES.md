# Deleting Landing Zones

## Background

Deleting resources in a landing zone presents several challenges:

 - Order of deletion is important. Many resources have a hard dependency on each other, and you can't delete a resource without deleting its dependent resource first. Unfortunately, the ARM API does not expose an easy way to list all these dependencies. Therefore the implementation must identify each dependency case explicitly.

 - Guarantee safety when deleting the landing zone resources. Resources in the workspace could use the resources in the landing zone, and when this is the case, you can't delete the landing zone. The definition of "usage" is not consistent and is specific to the resource. For example, an Azure Relay namespace is in use if a hybrid connection exists; in contrast, a VNet is considered in use if there's a VM attached to one of its subnets.

 - Landing zone definitions are extendable. Landing zones can be modified with new resources or definitions. Therefore, resource deletion must be flexible to address future scenarios.

## Implementation

### Deletion Order

To address the deletion order, the implementation groups the resource into two groups. The first group is the dependent resources, and the second is the base resources. Base resources are deleted last. 

The enum `LandingZoneBaseResourceType` contains the list of base resources, currently on a VNet is considered a base resource. As definitions evolved, additional base resources can be added to the enum.

Private endpoints are a special case. They must be deleted before a dependent and the base resources. The implementation identifies all the landing zone resources with a private endpoint to handle this case.

### Deletion Safety and Flexibility

To facilitate creating checks for usage dependencies between resources in a workspace and the landing zone. These checks are implemented as DeleteRules. These rules are applied to each resource, and all must pass before starting the delete process.

A rule implements:

```java
public interface DeleteRule {
  DeleteRuleResult applyRule(ResourceToDelete genericResource);
}

```

And to make the implementation of rules simpler, a base class is included with ARM clients and basic checks: `ResourceDependencyDeleteRule`.

A rule then can be implemented focusing on the specific resource that applies, for example a rule that checks if an Azure Relay namespace has hybrid connections.

```java
public class AzureRelayHasHybridConnections extends ResourceDependencyDeleteRule {
  public AzureRelayHasHybridConnections(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public String getExpectedType() {
    return AZURE_RELAY_TYPE;
  }

  @Override
  public boolean hasDependentResources(ResourceToDelete resourceToDelete) {
    return armManagers
        .relayManager()
        .hybridConnections()
        .listByNamespace(
            resourceToDelete.resource().resourceGroupName(), resourceToDelete.resource().name())
        .stream()
        .findAny()
        .isPresent();
  }
}
```

The rule based dependency checks are a flexible approach to implement additional checks as required.
