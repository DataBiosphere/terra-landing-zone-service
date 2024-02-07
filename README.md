
# Landing Zones

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DataBiosphere_terra-landing-zone-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DataBiosphere_terra-landing-zone-service)

## Overview

A Landing Zone is a set of cloud resources that serve as the underlying infrastructure where workspaces or other Terra
applications can
be deployed. The resources in a Landing Zone define and implement constraints, provide cross-cutting features, or can be
shared. These resources have a different lifecycle than resources in workspaces.

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

## Landing Zone Manager

The Landing Zone Manager is the high-level component that lists the available Landing Zone Step Definition factories, deploys
Landing Zone and lists resources per purpose.

The Landing Zone Manager requires a `TokenCredential`, `AzureProfile` and `resourceGroupName`.

```java
landingZoneManager=
        LandingZoneManager.createLandingZoneManager(
        credential,
        azureProfile,
        resourceGroupName);
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

You can get all available Landing Zone Step Factories:

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
      <td valign="top"><small>ProtectedDataResourcesFactory</small></td>
      <td valign="top">Deploy additional resources to the landing zone for additional security monitoring (using Azure Sentinel) and exporting logs to centralized long-term storage for retention.</td>
      <td valign="top">V1</td>
      <td valign="top">
            <ul>
                <li>All resources as in CromwellBaseResourcesFactory</li>
                <li>Long term storage account</li>
                <li>Additional AKS log configuration</li>
            </ul>
       </td>
        <td valign="top">
            <strong>Same as for CromwellBaseResourcesFactory</strong>
        </td>
    </tr>
  </tbody>
</table>

## Development

### Requirements

- Java 17
- Make sure [git-secrets](https://github.com/awslabs/git-secrets) installed. This tool prevents developers from committing passwords and secrets to git.

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