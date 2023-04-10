package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionContext;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment;

class ProtectedDataResourcesDefinitionV1 extends CromwellBaseResourcesDefinitionV1 {

  public ProtectedDataResourcesDefinitionV1(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public LandingZoneDeployment.DefinitionStages.Deployable definition(
      DefinitionContext definitionContext) {
    var base = super.definition(definitionContext);

    // Delete landing zone should be validated once we introduce new resources here.
    // We might need to implement new delete rules at
    // java/bio/terra/landingzone/library/landingzones/management/deleterules
    // Please check ResourcesDeleteManager class

    /*TEST add resources specific to Protected Data LZ*/
    //    var disk =
    //        armManagers
    //            .computeManager()
    //            .disks()
    //            .define("DummyDisk")
    //            .withRegion(definitionContext.resourceGroup().region())
    //            .withExistingResourceGroup(definitionContext.resourceGroup())
    //            .withData()
    //            .withSizeInGB(120)
    //            .withTag("purpose", "disk resource is specific to Protected Data LZ");
    //
    //    base.withResourceWithPurpose(disk, ResourcePurpose.WLZ_RESOURCE);
    return base;
  }
}
