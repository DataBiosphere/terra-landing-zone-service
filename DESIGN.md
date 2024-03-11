# Design

## Implementing a Landing Zone

The landing zone creation process is implemented as a [Stairway](https://github.com/DataBiosphere/stairway) flight, represented by the class `CreateLandingZoneFlight`. This flight consists of different steps one, of which is a step  
representing a sub-flight that creates the Azure resources (`CreateLandingZoneResourcesFlight`). `CreateLandingZoneResourcesFlight` utilizes an implementation of `StepsDefinitionProvider` to define the list of steps required to create the Azure resources.

Below are lists of changes required to introduce new landing zone:
1) Implement specific steps which are responsible for Azure resource creation (see example below for `CreateVnetStep`).
2) Create an implementation of `StepsDefinitionProvider` to define the list of Azure resource creation steps.
3) Introduce new landing zone type by extending `StepsDefinitionFactoryType`.
4) Update the mapping at `LandingZoneStepsDefinitionProviderFactory` based on new landing zone type. This mapping is used by the sub-flight `CreateLandingZoneResourcesFlight` to get the steps to create specific Azure resources.

In the case of updating an existing landing zone, it is required to do following:
1) Introduce new or adjusting existing step(s).
2) In case of new steps, it is also required to include them into the corresponding implementation of `StepsDefinitionProvider`.

### Landing Zone Flight Step Providers

Landing zones are implemented using providers classes which return the list of necessary steps to create a landing zone. Each provider should implement the interface [StepsDefinitionProvider](https://github.com/DataBiosphere/terra-landing-zone-service/blob/main/library/src/main/java/bio/terra/landingzone/library/landingzones/definition/factories/StepsDefinitionProvider.java).

### Azure Resource Flight Step
Each step is responsible to create a certain Azure resource and is represented as a separate class. All steps are inherited from the base class named `BaseResourceCreateStep`.
All steps are located in the following package `bio.terra.landingzone.stairway.flight.create.resource.step`. If a certain landing zone needs a new resource it is required only to introduce specific step and include it into specific provider.

Below is an example of a step responsible for vnet creation. `createResource` is the central part of the class implementation and it contains the logic responsible for Azure resource creation.
```java
public class CreateVnetStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreateVnetStep.class);
  public static final String VNET_ID = "VNET_ID";
  public static final String VNET_RESOURCE_KEY = "VNET";

  public CreateVnetStep(
          ArmManagers armManagers,
          ParametersResolver parametersResolver,
          ResourceNameProvider resourceNameProvider) {
    super(armManagers, parametersResolver, resourceNameProvider);
  }

  @Override
  public void createResource(FlightContext context, ArmManagers armManagers) {
    String vNetName = resourceNameProvider.getName(getResourceType());
    var nsgId =
            getParameterOrThrow(
                    context.getWorkingMap(), CreateNetworkSecurityGroupStep.NSG_ID, String.class);
    Network vNet = createVnetAndSubnets(context, armManagers, vNetName, nsgId);
    ...
    ...

    private Network createVnetAndSubnets(
            FlightContext context,
            ArmManagers armManagers,
            String vNetName,
            String networkSecurityGroupId) {
      var landingZoneId =
              getParameterOrThrow(
                      context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

      try {
        return armManagers
                .azureResourceManager()
                .networks()
                .define(vNetName)
                .withRegion(getMRGRegionName(context))
                .with()
                ...
                .create()
      ...
        
    @Override
    public String getResourceType() {
      return "VirtualNetwork";
    }

    ...
        
    @Override
    public List<ResourceNameRequirements> getResourceNameRequirements() {
      return List.of(
              new ResourceNameRequirements(
                      getResourceType(), ResourceNameGenerator.MAX_VNET_NAME_LENGTH));
    }
```

It is important that step's implementation should be idempotent. Please take a look at Stairway developer guide [here](https://github.com/DataBiosphere/stairway/blob/develop/FLIGHT_DEVELOPER_GUIDE.md).

Resource creation is defined using the standard Azure Java SDK. Together with defining desired properties for a resource it is also important to assign specific tags to a resource.

### Receiving Parameters

Each step has access to [ParameterResolver](https://github.com/DataBiosphere/terra-landing-zone-service/blob/main/library/src/main/java/bio/terra/landingzone/library/landingzones/definition/factories/ParametersResolver.java). This class allows accessing specific parameters. All default values for parameters are set in `LandingZoneDefaultParameters`.

### Naming Resources and Idempotency

Each step which creates an Azure resource should provide requirements for name generation. For doing this, it should provide a unique resource type (unique across all steps within the flight)
and also return a list of `ResourceNameRequirements` (in general, a step can create more than one Azure resource). Please take a look at the step example above.

The resource name generator creates a name from a hash of the landing zone id and internal sequence number.
As long as the landing zone id is globally unique, the resulting name will be the same across retry attempts with a very
low probability of a naming collision.

### Error handling and retrying

Each step is responsible for error handling. In the case that a step is responsible for creation of just one resource (recommended) there is usually no need to add additional error handling,
but this can be changed depending on complexity/requirements. The base step from which all steps are inherited contains some common error handling as well;
for instance, the base class handles the situation when the resource already exists.

## Deleting Landing Zones

Deleting resources in a landing zone presents several challenges:

- Order of deletion is important. Many resources have a hard dependency on each other, and you can't delete a resource without deleting its dependent resource first. Unfortunately, the ARM API does not expose an easy way to list all these dependencies. Therefore the implementation must identify each dependency case explicitly.

- Guarantee safety when deleting the landing zone resources. Resources in the workspace could use the resources in the landing zone, and when this is the case, you can't delete the landing zone. The definition of "usage" is not consistent and is specific to the resource. For example, an Azure Relay namespace is in use if a hybrid connection exists; in contrast, a VNet is considered in use if there's a VM attached to one of its subnets.

- Landing zone definitions are extendable. Landing zones can be modified with new resources or definitions. Therefore, resource deletion must be flexible to address future scenarios.

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
