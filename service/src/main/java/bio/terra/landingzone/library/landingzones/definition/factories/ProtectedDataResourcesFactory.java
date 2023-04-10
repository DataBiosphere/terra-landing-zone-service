package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.LandingZoneDefinable;
import java.util.List;

public class ProtectedDataResourcesFactory extends ArmClientsDefinitionFactory {
  private final String LZ_NAME = "Protected Data Landing Zone";
  private final String LZ_DESC = "Cromwell Landing Zone resources and some new resources (TBD)";

  public ProtectedDataResourcesFactory() {}

  public ProtectedDataResourcesFactory(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public DefinitionHeader header() {
    return new DefinitionHeader(LZ_NAME, LZ_DESC);
  }

  @Override
  public List<DefinitionVersion> availableVersions() {
    return List.of(DefinitionVersion.V1);
  }

  @Override
  public LandingZoneDefinable create(DefinitionVersion version) {
    if (version.equals(DefinitionVersion.V1)) {
      return new ProtectedDataResourcesDefinitionV1(armManagers);
    }
    throw new RuntimeException("Invalid Version");
  }
}
