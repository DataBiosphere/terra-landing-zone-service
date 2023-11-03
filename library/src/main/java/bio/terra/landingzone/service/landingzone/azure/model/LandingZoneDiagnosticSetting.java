package bio.terra.landingzone.service.landingzone.azure.model;

import com.azure.resourcemanager.monitor.models.LogSettings;
import java.util.List;

public record LandingZoneDiagnosticSetting(
    String resourceId, String name, List<LogSettings> logs) {}
