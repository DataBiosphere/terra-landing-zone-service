package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.LandingZoneDefinable;
import java.util.List;

public class FakeResourcesFactory extends ArmClientsDefinitionFactory {
  private final String LZ_NAME = "Fake Landing Zone Base Resources";
  private final String LZ_DESC = "Every resource which Cromwell has and disk";

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
      return new FakeResourcesFactoryDefinitionV1(armManagers);
    }
    throw new RuntimeException("Invalid Version");
  }
}
