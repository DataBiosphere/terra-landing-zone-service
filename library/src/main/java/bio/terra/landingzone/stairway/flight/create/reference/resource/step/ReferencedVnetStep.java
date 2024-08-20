package bio.terra.landingzone.stairway.flight.create.reference.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.deployment.SubnetResourcePurpose;
import bio.terra.landingzone.stairway.flight.LandingZoneDefaultParameters;
import bio.terra.stairway.FlightContext;
import java.util.Map;

public class ReferencedVnetStep extends BaseReferencedResourceStep {
  public ReferencedVnetStep(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  protected boolean isSharedResource() {
    return false;
  }

  @Override
  protected Map<String, String> getLandingZoneResourceTags(
      FlightContext context, String resourceId) {
    // get subnet parameters and set them as tags...
    return Map.of(
        SubnetResourcePurpose.WORKSPACE_BATCH_SUBNET.toString(),
        getParametersResolver(context)
            .getValue(LandingZoneDefaultParameters.ParametersNames.BATCH_SUBNET.name()),
        SubnetResourcePurpose.WORKSPACE_COMPUTE_SUBNET.toString(),
        getParametersResolver(context)
            .getValue(LandingZoneDefaultParameters.ParametersNames.COMPUTE_SUBNET.name()));
  }

  @Override
  protected ArmResourceType getArmResourceType() {
    return ArmResourceType.VNET;
  }
}
