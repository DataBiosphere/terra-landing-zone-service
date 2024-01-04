package bio.terra.landingzone.library.landingzones.definition;

import java.util.List;

// TODO: do we need className?
public record FactoryDefinitionInfo(
    String name, String description, String className, List<DefinitionVersion> versions) {}
