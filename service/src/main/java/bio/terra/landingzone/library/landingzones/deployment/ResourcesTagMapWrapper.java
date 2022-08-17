package bio.terra.landingzone.library.landingzones.deployment;

import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.relay.models.RelayNamespace;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.azure.resourcemanager.resources.fluentcore.model.Creatable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/** Maintains the tags and resources to be deployed in a deployment instance. */
public class ResourcesTagMapWrapper {

  private final Map<Creatable<?>, Map<String, String>> resourcesTagsMap = new HashMap<>();

  private final Map<RelayNamespace.DefinitionStages.WithCreate, Map<String, String>>
      relayResourcesTagsMap = new HashMap<>();
  private final Map<BatchAccount.DefinitionStages.WithCreate, Map<String, String>>
      batchResourcesTagsMap = new HashMap<>();
  private final Map<Server.DefinitionStages.WithCreate, Map<String, String>>
      postgresResourcesTagsMap = new HashMap<>();
  private final Map<PrivateEndpoint.DefinitionStages.WithCreate, Map<String, String>>
      privateEndpointResourcesTagsMap = new HashMap<>();
  private final ClientLogger logger = new ClientLogger(ResourcesTagMapWrapper.class);
  private final String landingZoneId;

  ResourcesTagMapWrapper(String landingZoneId) {

    if (StringUtils.isBlank(landingZoneId)) {
      throw logger.logExceptionAsError(
          new IllegalArgumentException("Landing Zone ID is invalid. It can't be blank or null"));
    }

    this.landingZoneId = landingZoneId;
  }

  public String getLandingZoneId() {
    return landingZoneId;
  }

  private <T extends Creatable<?> & Resource.DefinitionWithTags<?>> void putTagKeyValue(
      T resource, String key, String value) {
    Map<String, String> tagMap = resourcesTagsMap.get(resource);
    if (tagMap == null) {
      tagMap = new HashMap<>();
    }

    tagMap.put(key, value);
    resourcesTagsMap.put(resource, tagMap);
  }

  <T extends Creatable<?> & Resource.DefinitionWithTags<?>> void putWithPurpose(
      T resource, ResourcePurpose purpose) {

    putWithLandingZone(resource);
    putTagKeyValue(
        resource, LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(), purpose.toString());
  }

  void putResource(Creatable<?> resource) {

    resourcesTagsMap.put(resource, null);
  }

  <T extends Creatable<?> & Resource.DefinitionWithTags<?>> void putWithLandingZone(T resource) {

    putTagKeyValue(resource, LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);
  }

  Map<Creatable<?>, Map<String, String>> getResourcesTagsMap() {
    return resourcesTagsMap;
  }

  Map<RelayNamespace.DefinitionStages.WithCreate, Map<String, String>> getRelayResourcesTagsMap() {
    return relayResourcesTagsMap;
  }

  Map<BatchAccount.DefinitionStages.WithCreate, Map<String, String>> getBatchResourcesTagsMap() {
    return batchResourcesTagsMap;
  }

  Map<Server.DefinitionStages.WithCreate, Map<String, String>> getPostgresResourcesTagsMap() {
    return postgresResourcesTagsMap;
  }

  Map<PrivateEndpoint.DefinitionStages.WithCreate, Map<String, String>>
      getPrivateEndpointResourcesTagsMap() {
    return privateEndpointResourcesTagsMap;
  }

  Map<String, String> getResourceTagsMap(Creatable<?> resource) {
    return resourcesTagsMap.get(resource);
  }

  void putWithVNetWithPurpose(
      Network.DefinitionStages.WithCreateAndSubnet virtualNetwork,
      String subnetName,
      SubnetResourcePurpose purpose) {
    putWithLandingZone(virtualNetwork);
    Map<String, String> tagMap = resourcesTagsMap.get(virtualNetwork);
    if (tagMap == null) {
      tagMap = new HashMap<>();
    }

    tagMap.put(purpose.toString(), subnetName);
    resourcesTagsMap.put(virtualNetwork, tagMap);
  }

  void putWithPurpose(RelayNamespace.DefinitionStages.WithCreate relay, ResourcePurpose purpose) {
    putTagKeyValue(relay, LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);
    putTagKeyValue(relay, LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(), purpose.toString());
  }

  void putWithPurpose(BatchAccount.DefinitionStages.WithCreate batch, ResourcePurpose purpose) {
    putTagKeyValue(batch, LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);
    putTagKeyValue(batch, LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(), purpose.toString());
  }

  void putWithPurpose(Server.DefinitionStages.WithCreate posgres, ResourcePurpose purpose) {
    putTagKeyValue(posgres, LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);
    putTagKeyValue(posgres, LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(), purpose.toString());
  }

  void putWithPurpose(
      PrivateEndpoint.DefinitionStages.WithCreate privateEndpoint, ResourcePurpose purpose) {
    putTagKeyValue(privateEndpoint, LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId);
    putTagKeyValue(
        privateEndpoint, LandingZoneTagKeys.LANDING_ZONE_PURPOSE.toString(), purpose.toString());
  }

  private void putTagKeyValue(
      RelayNamespace.DefinitionStages.WithCreate relay, String key, String value) {
    Map<String, String> tagMap = relayResourcesTagsMap.get(relay);
    if (tagMap == null) {
      tagMap = new HashMap<>();
    }

    tagMap.put(key, value);
    relayResourcesTagsMap.put(relay, tagMap);
  }

  private void putTagKeyValue(
      BatchAccount.DefinitionStages.WithCreate batch, String key, String value) {
    Map<String, String> tagMap = batchResourcesTagsMap.get(batch);
    if (tagMap == null) {
      tagMap = new HashMap<>();
    }

    tagMap.put(key, value);
    batchResourcesTagsMap.put(batch, tagMap);
  }

  private void putTagKeyValue(
      Server.DefinitionStages.WithCreate posgres, String key, String value) {
    Map<String, String> tagMap = postgresResourcesTagsMap.get(posgres);
    if (tagMap == null) {
      tagMap = new HashMap<>();
    }

    tagMap.put(key, value);
    postgresResourcesTagsMap.put(posgres, tagMap);
  }

  private void putTagKeyValue(
      PrivateEndpoint.DefinitionStages.WithCreate privateEndpoint, String key, String value) {
    Map<String, String> tagMap = privateEndpointResourcesTagsMap.get(privateEndpoint);
    if (tagMap == null) {
      tagMap = new HashMap<>();
    }

    tagMap.put(key, value);
    privateEndpointResourcesTagsMap.put(privateEndpoint, tagMap);
  }
}
