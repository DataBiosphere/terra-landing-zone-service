package bio.terra.landingzone.library.landingzones.management;

import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.resources.models.GenericResource;
import java.util.List;

/**
 * Represents a generic resource to be deleted and its private endpoint, if it exists.
 *
 * @param resource resource to delete.
 * @param privateEndpoint private endpoint associated with the resource.
 * @param solutions solutions associated with the resource. Currently, there are two types of
 *     solutions might be associated with the resource - ContainerInsights, SecurityInsights. Both
 *     have type 'Solution'.
 */
public record ResourceToDelete(
    GenericResource resource, PrivateEndpoint privateEndpoint, List<GenericResource> solutions) {}
