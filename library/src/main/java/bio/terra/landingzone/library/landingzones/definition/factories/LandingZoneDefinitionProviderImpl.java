package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import com.azure.core.util.logging.ClientLogger;

/** Implementation of {@link LandingZoneDefinitionProvider} */
public class LandingZoneDefinitionProviderImpl implements LandingZoneDefinitionProvider {

  private final ClientLogger logger = new ClientLogger(LandingZoneDefinitionProviderImpl.class);
  private final ArmManagers armManagers;

  public LandingZoneDefinitionProviderImpl(ArmManagers armManagers) {
    this.armManagers = armManagers;
  }

  @Override
  public <T extends LandingZoneDefinitionFactory>
      LandingZoneDefinitionFactory createDefinitionFactory(Class<T> factory) {
    return createNewFactoryInstance(factory);
  }

  private <T extends LandingZoneDefinitionFactory> T createNewFactoryInstance(
      Class<T> factoryClass) {
    try {
      return factoryClass.getDeclaredConstructor(ArmManagers.class).newInstance(armManagers);
    } catch (Exception e) {
      throw logger.logExceptionAsError(new RuntimeException(e));
    }
  }
}
