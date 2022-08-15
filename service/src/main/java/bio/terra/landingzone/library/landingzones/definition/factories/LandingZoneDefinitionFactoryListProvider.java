package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import java.util.List;

public interface LandingZoneDefinitionFactoryListProvider {
  public List<FactoryDefinitionInfo> listFactories();

  public List<Class<? extends LandingZoneDefinitionFactory>> listFactoriesClasses();
}
