package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.FactoryDefinitionInfo;
import com.azure.core.util.logging.ClientLogger;
import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class LandingZoneDefinitionFactoryListProviderImpl
    implements LandingZoneDefinitionFactoryListProvider {
  private final ClientLogger logger =
      new ClientLogger(LandingZoneDefinitionFactoryListProviderImpl.class);

  @Override
  public List<FactoryDefinitionInfo> listFactories() {
    try {
      String packageName = this.getClass().getPackageName();
      return ClassPath.from(ClassLoader.getSystemClassLoader())
          .getTopLevelClasses(packageName)
          .stream()
          .filter(this::isLandingZoneFactory)
          .map(c -> toFactoryDefinitionInfo((Class<? extends StepsDefinitionProvider>) c.load()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw logger.logExceptionAsError(new RuntimeException(e));
    }
  }

  @Override
  public List<Class<? extends StepsDefinitionProvider>> listFactoriesClasses() {
    try {
      String packageName = this.getClass().getPackageName();
      return ClassPath.from(ClassLoader.getSystemClassLoader())
          .getTopLevelClasses(packageName)
          .stream()
          .filter(this::isLandingZoneFactory)
          .map(c -> (Class<? extends StepsDefinitionProvider>) c.load())
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw logger.logExceptionAsError(new RuntimeException(e));
    }
  }

  private <T extends StepsDefinitionProvider> FactoryDefinitionInfo toFactoryDefinitionInfo(
      Class<T> factoryClass) {
    StepsDefinitionProvider factory;
    try {
      factory = createNewFactoryInstance(factoryClass);
    } catch (Exception e) {
      throw logger.logExceptionAsError(new RuntimeException(e));
    }
    return new FactoryDefinitionInfo(
        factory.header().definitionName(),
        factory.header().definitionDescription(),
        factory.landingZoneDefinition(),
        factory.availableVersions());
  }

  private boolean isLandingZoneFactory(ClassPath.ClassInfo classInfo) {
    // a factory is a non-abstract class that implements StepsDefinitionProvider.
    return !classInfo.load().isInterface()
        && StepsDefinitionProvider.class.isAssignableFrom(classInfo.load())
        && !Modifier.isAbstract(classInfo.load().getModifiers());
  }

  private <T extends StepsDefinitionProvider> T createNewFactoryInstance(Class<T> factoryClass) {
    try {
      return factoryClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw logger.logExceptionAsError(new RuntimeException(e));
    }
  }
}
