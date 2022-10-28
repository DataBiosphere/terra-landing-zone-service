package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.UUID;

public record StartLandingZoneCreation(UUID landingZoneId, String definition, String version) {}
