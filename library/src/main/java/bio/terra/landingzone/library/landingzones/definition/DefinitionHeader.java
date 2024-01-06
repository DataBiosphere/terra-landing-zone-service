package bio.terra.landingzone.library.landingzones.definition;

/**
 * Represents code name of a landing zone together with description. Information header for a {@link
 * bio.terra.landingzone.library.landingzones.definition.factories.StepsDefinitionProvider}
 */
public record DefinitionHeader(String definitionName, String definitionDescription) {}
