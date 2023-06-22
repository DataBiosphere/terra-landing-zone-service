package bio.terra.landingzone.stairway.flight;

/**
 * Represents a requirements for resource name generation during LZ resource creation flight. This
 * can be extended in future in case we have requirements for a minimal length of a name or anything
 * else.
 *
 * @param resourceType Uniquely identify resource for which name should be generated.
 * @param maxNameLength The maximum length of a name.
 */
public record ResourceNameRequirements(String resourceType, int maxNameLength) {}
