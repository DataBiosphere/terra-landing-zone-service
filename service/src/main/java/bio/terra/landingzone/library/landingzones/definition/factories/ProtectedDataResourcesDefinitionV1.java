package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionContext;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;

// Definition of Fake LZ contains all the resource from the CromwellBase LZ, plus additional
// resources
class ProtectedDataResourcesDefinitionV1 extends CromwellBaseResourcesDefinitionV1 {

  public ProtectedDataResourcesDefinitionV1(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public LandingZoneDeployment.DefinitionStages.Deployable definition(
      DefinitionContext definitionContext) {
    var base = super.definition(definitionContext);

    /*add resources specific to Fake LZ*/

    var disk =
        armManagers
            .computeManager()
            .disks()
            .define("FakeLZDisk")
            .withRegion(definitionContext.resourceGroup().region())
            .withExistingResourceGroup(definitionContext.resourceGroup())
            .withData()
            .withSizeInGB(120)
            .withTag("purpose", "disk is specific to Fake LZ");

    base.withResourceWithPurpose(disk, ResourcePurpose.WLZ_RESOURCE);
    return base;
  }
}
