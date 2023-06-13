package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.stairway.flight.create.resource.step.BaseResourceCreateStep;
import bio.terra.landingzone.stairway.flight.exception.ResourceNameGenerationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Provides name for a resource during LZ resource creation flight. */
public class ResourceNameProvider {
  private final ResourceNameGenerator resourceNameGenerator;
  // key is resource type, value is corresponding name
  private final Map<String, String> resourceNames;

  public ResourceNameProvider(UUID landingZoneId) {
    this.resourceNameGenerator = new ResourceNameGenerator(landingZoneId.toString());
    resourceNames = new HashMap<>();
  }

  /** Register LZ flight step for resource name generation. */
  public void registerStep(BaseResourceCreateStep step) {
    var resourceNameRequirements = step.getResourceNameRequirements();
    resourceNameRequirements.forEach(
        nameRequirements -> {
          if (resourceNames.containsKey(nameRequirements.resource())) {
            throw new ResourceNameGenerationException(
                String.format(
                    "Step with resource %s is already registered for name generation.",
                    nameRequirements.resource()));
          }
          resourceNames.put(
              nameRequirements.resource(),
              resourceNameGenerator.nextName(nameRequirements.maxNameLength()));
        });
  }

  public String getName(String resource) {
    if (resourceNames.containsKey(resource)) {
      return resourceNames.get(resource);
    }
    throw new ResourceNameGenerationException(
        String.format("Step with resource %s is not registered for name generation.", resource));
  }
}
