package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import java.util.List;

public interface LandingZoneDefinitionFactoryListProvider {
  List<FactoryDefinitionInfo> listFactories();

  List<Class<? extends LandingZoneDefinitionFactory>> listFactoriesClasses();
}
