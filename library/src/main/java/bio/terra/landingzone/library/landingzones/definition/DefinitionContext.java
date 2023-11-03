package bio.terra.landingzone.library.landingzones.definition;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment.DefinitionStages.WithLandingZoneResource;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import java.util.Map;

/** Context in which the deployment must occur. */
public record DefinitionContext(
    String landingZoneId,
    WithLandingZoneResource deployment,
    ResourceGroup resourceGroup,
    ResourceNameGenerator resourceNameGenerator,
    Map<String, String> parameters) {}
