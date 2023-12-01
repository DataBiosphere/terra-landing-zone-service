
# Landing Zones

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DataBiosphere_terra-landing-zone-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DataBiosphere_terra-landing-zone-service)

## Overview

A Landing Zone is a set of cloud resources that serve as the underlying infrastructure where workspaces or other Terra
applications can
be deployed. The resources in a Landing Zone define and implement constraints, provide cross-cutting features, or can be
shared. These resources have a different lifecycle than resources in workspaces.

## Implementing a Landing Zone

### Landing Zone Definition Factories and Landing Zone Definitions.

Landing zones are implemented using the factory pattern; the factory creates *Landing Zone Definitions* (LZDs).

Landing Zone Definitions are where resources and their purpose are defined.

A Landing Zone Definition factory is an implementation of:

```java
public interface LandingZoneDefinitionFactory {
    DefinitionHeader header();

    List<DefinitionVersion> availableVersions();

    LandingZoneDefinable create(DefinitionVersion version);
}
```

The library includes an abstract class that expects the Azure Resource Manager (ARM)
clients: `ArmClientsDefinitionFactory`. Factories should extend this class.

In addition, factories:

- Must be implemented in the `factories` package.
- Must have a package scoped parameterless constructor.

```java
package bio.terra.landingzone.library.landingzones.definition.factories;


public class FooLZFactory extends ArmClientsDefinitionFactory {

    FooLZFactory() {
    }

    @Override
    public DefinitionHeader header() {
        return new DefinitionHeader("Foo LZ", "Description of Foo LZ");
    }

    @Override
    public List<DefinitionVersion> availableVersions() {
        return List.of(DefinitionVersion.V1);
    }

    @Override
    public LandingZoneDefinable create(DefinitionVersion version) {
        if (version.equals(DefinitionVersion.V1)) {
            return new FooLZDefinitionV1(azureResourceManager, relayManager);
        }
        throw new RuntimeException("Invalid Version");
    }
}
```

An inner class in the factory class is a good convention for implementing a Landing Zone Definition.

```java
public class FooLZFactory extends ArmClientsDefinitionFactory {
    ...

    class FooLZDefinitionV1 extends LandingZoneDefinition {

        protected FooLZDefinitionV1(
                AzureResourceManager azureResourceManager, RelayManager relayManager) {
            super(azureResourceManager, relayManager);
        }

        @Override
        public Deployable definition(DefinitionContext definitionContext) {
            var storage =
                    azureResourceManager
                            .storageAccounts()
                            .define(definitionContext.resourceNameGenerator().nextName(20))
                            .withRegion(Region.US_EAST2)
                            .withExistingResourceGroup(definitionContext.resourceGroup());

            var vNet =
                    azureResourceManager
                            .networks()
                            .define(definitionContext.resourceNameGenerator().nextName(20))
                            .withRegion(Region.US_EAST2)
                            .withExistingResourceGroup(definitionContext.resourceGroup())
                            .withAddressSpace("10.0.0.0/28")
                            .withSubnet("compute", "10.0.0.0/29")
                            .withSubnet("storage", "10.0.0.8/29");

            return definitionContext
                    .deployment()
                    .withResourceWithPurpose(storage, ResourcePurpose.SHARED_RESOURCE)
                    .withVNetWithPurpose(vNet, "compute", SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET)
                    .withVNetWithPurpose(vNet, "storage", SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET);
        }
    }
```

Resources are defined using the standard Azure Java SDK but with the following constraints to consider:

- The purpose of the resources must be indicated in the deployment.
- Resources not included in the deployment won't be created.
- The `create()` method in the resource definition must not be called.
- The resource definition must have the required configuration for a creation before it can be added to the deployment.

### Receiving Parameters

The `DefinitionContext` contains the property `Map<String, String> parameters`, which can be used to receive parameter
for the definition of a landing zone.

A few points to consider when implementing a definition that requires parameters:

- You must add the necessary validation for these parameters.
- `parameters` can be  `null`

### Naming Resources and Idempotency

You can use the resource name generator in the deployment context to guarantee that names are consistent in retry
attempts.

The resource name generator creates a name from a hash of the landing zone id and internal sequence number.
As long as the landing zone id is globally unique, the resulting name will be the same across retry attempts with a very
low probability of a naming collision.

> The Azure Resource Manager APIs can be retried if a transient error occurs - the API is idempotent. However, The
> request must be the same as the failed one to avoid duplicating resources in the deployment. The deployment could
> create
> duplicate resources if the resource's name is auto-generated and changes in every request.

An instance of the resource name generator is included in the deployment context.

```java
 var storage=azureResourceManager
        .storageAccounts()
        .define(definitionContext.resourceNameGenerator().nextName(20))
        .withRegion(Region.US_EAST2)
        .withExistingResourceGroup(definitionContext.resourceGroup());

```

### Handling Prerequisites

The library deploys resources in a non-deterministic order. Therefore, it is not possible to assume any specific order.
For cases when a resource must be created before other resources, you can create a prerequisite deployment inside your
definition.

```java

class FooLZDefinitionV1 extends LandingZoneDefinition {

    protected FooLZDefinitionV1(
            AzureResourceManager azureResourceManager, RelayManager relayManager) {
        super(azureResourceManager, relayManager);
    }

    @Override
    public Deployable definition(DefinitionContext definitionContext) {

        var vNet =
                azureResourceManager
                        .networks()
                        .define(definitionContext.resourceNameGenerator().nextName(20))
                        .withRegion(Region.US_EAST2)
                        .withExistingResourceGroup(definitionContext.resourceGroup())
                        .withAddressSpace("10.0.0.0/28")
                        .withSubnet("subnet1", "10.0.0.0/29")

        var prerequisites =
                deployment
                        .definePrerequisites()
                        .withVNetWithPurpose(vNet, "subnet1", SubnetResourcePurpose.WORKSPACE_STORAGE_SUBNET)
                        .deploy();

        var storage =
                azureResourceManager
                        .storageAccounts()
                        .define(definitionContext.resourceNameGenerator().nextName(20))
                        .withRegion(Region.US_EAST2)
                        .withExistingResourceGroup(definitionContext.resourceGroup());

        return definitionContext
                .deployment()
                .withResourceWithPurpose(storage, ResourcePurpose.SHARED_RESOURCE);
    }
```

## Landing Zone Manager

The Landing Zone Manager is the high-level component that lists the available Landing Zone Definition factories, deploys
Landing Zone Definitions and lists resources per purpose.

The Landing Zone Manager requires a `TokenCredential`, `AzureProfile` and `resourceGroupName`.

```java
landingZoneManager=
        LandingZoneManager.createLandingZoneManager(
        credential,
        azureProfile,
        resourceGroupName);
```

### Deploying a Landing Zone Definition

You can deploy Landing Zone Definition using the manager.

```java
    List<DeployedResource> resources=
        landingZoneManager.deployLandingZone(
        landingZoneId,
        "FooLZFactory",
        DefinitionVersion.V1,
        parameters);

```

The manager has an asynchronous API for deployments. You can implement retry capabilities using standard reactive retry
policies.

```java
    Flux<DeployedResource> resources=
        landingZoneManager
        .deployLandingZoneAsync(landingZoneId,FooLZDefinitionV1.class,DefinitionVersion.V1,parameters)
        .retryWhen(Retry.max(1));

```

### Reading Landing Zone Resources

You can list resources by purpose using the Landing Zone Manager:

```java
List<DeployedResource> resources=landingZoneManager.reader().listResourcesByPurpose(landingZoneId, ResourcePurpose.SHARED_RESOURCE);

```

Virtual Networks can be listed by subnet purpose:

```java
    List<DeployedVNet> vNets=
        landingZoneManager.reader().listVNetWithSubnetPurpose(landingZoneId, SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET);

```

### Getting the available Landing Zone Factories

You can get all available Landing Zone Factories:

```java

List<FactoryInfo> factories=LandingZoneManager.listDefinitionFactories();

```
## Landing Zone Service

The Landing Zone Service is a Spring service component that wraps the Landing Zone Manager, provides a job based/async API to deploy Landing Zones, and persists Landing Zone deployments state in a DB.


## Landing Zone Definitions

The table below describes the current Landing Zone Definitions available in the library.

<table>
    <thead><tr>
      <th>Factory</th>
      <th>Description</th>
      <th>Versions</th>
      <th>Shared Resources</th>
      <th>Parameters</th>
    </tr>
    </thead>
    <tbody>
    <tr>
      <td valign="top"><small>CromwellBaseResourcesFactory</small></td>
      <td valign="top">Deploys required resources to run Cromwell on Azure.</td>
      <td valign="top">V1</td>
      <td valign="top">
            <ul>
                <li>AKS Cluster</li>
                <li>Batch Account</li>
                <li>Storage Account</li>
                <li>PostgreSQL</li>
                <li>Azure Relay Namespace</li>
                <li>VNet with subnets for PostgreSQL, AKS Node pool, PostgreSQL databases and Compute resources</li>
            </ul>
       </td>
        <td valign="top">
            <strong>POSTGRES_DB_ADMIN:</strong> Username of the DB admin<br/>Default value: <i>db_admin</i><br/><br/>
            <strong>POSTGRES_DB_PASSWORD:</strong> DB admin password <br/>Default value: <i>UUID.randomUUID().toString()</i><br/><br/>
            <strong>POSTGRES_SERVER_SKU:</strong> PostgreSQL Server SKU <br/>Default value: <i>Standard_D2ds_v5</i><br/><br/>
            <strong>POSTGRES_SERVER_SKU_TIER:</strong> PostgreSQL Server Compute Tier <br/>Default value: <i>General Purpose</i><br/><br/>
            <strong>VNET_ADDRESS_SPACE:</strong> Virtual network address space <br/>Default value: <i>10.1.0.0/27</i><br/><br/>
            <strong>AKS_SUBNET:</strong> AKS subnet address space <br/>Default value: <i>10.1.0.0/29</i><br/><br/>
            <strong>BATCH_SUBNET:</strong> Batch subnet address space <br/>Default value: <i>10.1.0.8/29</i><br/><br/>
            <strong>POSTGRESQL_SUBNET:</strong> PostgreSQL subnet address space <br/>Default value: <i>10.1.0.16/29</i><br/><br/>
            <strong>COMPUTE_SUBNET:</strong> Compute resources subnet address space <br/>Default value: <i>10.1.0.24/29</i><br/><br/>
            <strong>AKS_NODE_COUNT:</strong> Number of nodes in AKS Nodepool  <br/>Default value: <i>1</i><br/><br/>
            <strong>AKS_MACHINE_TYPE:</strong> Machine type used for AKS hosts <br/>Default value: <i>ContainerServiceVMSizeTypes.STANDARD_A2_V2</i><br/><br/>
            <strong>AKS_AUTOSCALING_ENABLED:</strong> Flag to enabled autoscaling for the AKS nodepool <br/>Default value: <i>false</i><br/><br/>
            <strong>AKS_AUTOSCALING_MIN:</strong> Minimum number of nodes in nodepool when autoscaling is enabled <br/>Default value: <i>1</i><br/><br/>
            <strong>AKS_AUTOSCALING_MAX:</strong> Maximum number of nodes in nodepool when autoscaling is enabled <br/>Default value: <i>3</i><br/><br/>
            <strong>AKS_COST_SAVING_SPOT_NODES_ENABLED:</strong> Enable Spot Node usage on AKS <br/>Default value: <i>false</i><br/><br/>
            <strong>Azure storage account overview:</strong> <a href="https://learn.microsoft.com/en-us/azure/storage/common/storage-account-overview">documentation</a><br/><br/>
            <strong>Azure storage CORS configuration:</strong> <a href="https://learn.microsoft.com/en-us/rest/api/storageservices/cross-origin-resource-sharing--cors--support-for-the-azure-storage-services">documentation</a><br/><br/>
            <strong>STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS:</strong> The origin domains that are permitted to make a request against the storage service via CORS <br/>Default value: <i>*</i><br/><br/>
            <strong>STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS:</strong> The methods (HTTP request verbs) that the origin domain may use for a CORS request <br/>Default value: <i>GET,HEAD,OPTIONS,PUT,PATCH,POST,MERGE,DELETE</i><br/><br/>
            <strong>STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS:</strong> The request headers that the origin domain may specify on the CORS request <br/>Default value: <i>authorization,content-type,x-app-id,Referer,x-ms-blob-type,x-ms-copy-source,content-length</i><br/><br/>
            <strong>STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS:</strong> The response headers that may be sent in the response to the CORS request and exposed by the browser to the request issuer <br/>Default value: <i>Empty string</i><br/><br/>
            <strong>STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE:</strong> The maximum amount time that a browser should cache the preflight OPTIONS request (in seconds) <br/>Default value: <i>0</i><br/><br/>
            <strong>Azure storage SKU types:</strong> <a href="https://learn.microsoft.com/en-us/rest/api/storagerp/srp_sku_types">documentation</a><br/><br/>
            <strong>STORAGE_ACCOUNT_SKU_TYPE:</strong> Type of storage account <br/>Default value: <i>Standard_LRS</i>; <br/>Accepted values: <i>Standard_LRS</i>, <i>Standard_GRS</i>, <i>Standard_RAGRS</i>, <i>Standard_ZRS</i>, <i>Premium_LRS;</i> Please see StorageAccountSkuType;<br/><br/>
            <strong>ENABLE_PGBOUNCER:</strong> Whether to have pgbouncer enabled on postgresql server <br/>Default value: <i>true</i>;
        </td>
    </tr>
<tr>
      <td valign="top"><small>ManagedNetworkWithSharedResourcesFactory</small></td>
      <td valign="top">Deploys a virtual network, shared storage and Azure Relay namespace.</td>
      <td valign="top">V1</td>
      <td valign="top">
            <ul>
                <li>Storage Account</li>
                <li>Azure Relay Namespace</li>
                <li>VNet</li>
            </ul>
       </td>
        <td valign="top">
            <strong>NA</strong>
        </td>
    </tr>
  </tbody>
</table>

## Testing

The Landing Zone Service contains unit and integration tests. These tests are run as part of the CI pipeline during the PR process,
as well as on merge to `main`.

### Local Testing

```sh
# Unit tests
./gradlew :library:unitTest

# integration tests
./gradlew :library:integrationTest
```

#### Setup for Local Integration Testing
Running integration tests locally requires:
* A credential capable of connecting to the subscription and tenant configured [here](https://github.com/DataBiosphere/terra-landing-zone-service/blob/main/service/src/test/java/bio/terra/landingzone/library/landingzones/AzureIntegrationUtils.java#L27).
  In CI, we have a federated identity configured which logs in and sets the appropriate environment variables. For local testing,
  the Azure CLI is the best way to get the needed environment variables set via an invocation of `az login`. For more information,
  see the related Azure [documentation](https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#defaultazurecredential).

* A running postgres:
```
 ./library/local-dev/run_postgres.sh start|stop
```