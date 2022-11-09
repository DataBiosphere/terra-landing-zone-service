package bio.terra.landingzone.library.landingzones.management;

import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.resources.models.GenericResource;

/**
 * Represents a generic resource to be deleted and its private endpoint, if it exists.
 *
 * @param resource resource to delete.
 * @param privateEndpoint private endpoint associated with the resource.
 */
public record ResourceToDelete(
    GenericResource resource, PrivateEndpoint privateEndpoint, GenericResource solution) {}
