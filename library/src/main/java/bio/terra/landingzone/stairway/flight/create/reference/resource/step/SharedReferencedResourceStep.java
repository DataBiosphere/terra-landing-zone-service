package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.stairway.FlightContext;
import java.util.Map;

public class SharedReferencedResourceStep extends BaseReferencedResourceStep {
  protected SharedReferencedResourceStep(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  protected boolean isSharedResource() {
    return true;
  }

  @Override
  protected Map<String, String> getLandingZoneResourceTags(
      FlightContext context, String resourceId) {
    return Map.of();
  }

  @Override
  protected ArmResourceType getArmResourceType() {
    return null;
  }
}
