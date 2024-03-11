
# Landing Zones

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DataBiosphere_terra-landing-zone-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DataBiosphere_terra-landing-zone-service)

## Overview

A Landing Zone is a set of cloud resources that serve as the underlying infrastructure where workspaces or other Terra
applications can
be deployed. The resources in a Landing Zone define and implement constraints, provide cross-cutting features, or can be
shared. These resources have a different lifecycle than resources in workspaces.

## Current state

The Landing Zone "Service" is currently being used as a library attached to Workspace Manager. It is in the process of being stood up as a standalone service (see the stub service [README.md](./service/README.md)).

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

## Additional information

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for more information about the process of
running tests and contributing code to the repository and [DESIGN.md](./DESIGN.md) for a deeper understanding of the
repository's structure and design patterns.
