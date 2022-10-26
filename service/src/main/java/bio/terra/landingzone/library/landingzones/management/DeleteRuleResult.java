package bio.terra.landingzone.library.landingzones.management;

public record DeleteRuleResult(
    boolean isDeletable, String ruleName, String reason, String resourceType) {}
