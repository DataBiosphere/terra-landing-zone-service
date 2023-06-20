package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.stairway.flight.create.resource.step.BaseResourceCreateStep;
import bio.terra.landingzone.stairway.flight.exception.ResourceNameGenerationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provides name for a resource during LZ resource creation flight.
 *
 * <p>Each flight's step creates a resource or resources which require a unique name. Step should
 * register itself in the ResourceNameProvider to be able to request a name(s) from it. All steps
 * which inherit BaseResourceCreateStep are registered by default. Name generation is based on
 * requirements provided by a step. Each step should uniquely identify resource(s) which would be
 * created. For doing this it provides requirements for name generation. Requirements are
 * represented by a pair of unique name (resourceType) which helps identify a resource for which a
 * new name will be generated and maximum length of requested name. Some steps might claim multiple
 * names (like CreateAksStep). In this case resourceType represents a main resource and is used
 * together with some other unique string to represent an auxiliary resource.
 */
public class ResourceNameProvider {
  private final ResourceNameGenerator resourceNameGenerator;
  // key is resource type, value is corresponding name
  private final Map<String, String> resourceTypeNames;

  public ResourceNameProvider(UUID landingZoneId) {
    this.resourceNameGenerator = new ResourceNameGenerator(landingZoneId.toString());
    resourceTypeNames = new HashMap<>();
  }

  /** Register LZ flight step for resource name generation. */
  public void registerStep(BaseResourceCreateStep step) {
    var resourceNameRequirements = step.getResourceNameRequirements();
    resourceNameRequirements.forEach(
        nameRequirements -> {
          if (resourceTypeNames.containsKey(nameRequirements.resourceType())) {
            throw new ResourceNameGenerationException(
                String.format(
                    "Step with resource type '%s' is already registered for name generation.",
                    nameRequirements.resourceType()));
          }
          resourceTypeNames.put(
              nameRequirements.resourceType(),
              resourceNameGenerator.nextName(nameRequirements.maxNameLength()));
        });
  }

  public String getName(String resourceType) {
    if (resourceTypeNames.containsKey(resourceType)) {
      return resourceTypeNames.get(resourceType);
    }
    throw new ResourceNameGenerationException(
        String.format(
            "Step with resource type '%s' is not registered for name generation.", resourceType));
  }
}
