package bio.terra.landingzone.library.landingzones.definition;

import java.util.List;

public record FactoryDefinitionInfo(
    String name, String description, String className, List<DefinitionVersion> versions) {}
