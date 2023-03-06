package bio.terra.landingzone.service.landingzone.azure.model;

import java.util.List;
import java.util.UUID;

public record DeletedLandingZone(
    UUID landingZoneId, List<String> deleteResources, UUID billingProfileId) {}
