package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.configuration.LandingZoneAzureRegionConfiguration;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParametersResolverProvider {

  private final String GLOBAL_KEY = "global";
  private final LandingZoneAzureRegionConfiguration landingZoneAzureRegionConfiguration;

  @Autowired
  public ParametersResolverProvider(
      LandingZoneAzureRegionConfiguration landingZoneAzureRegionConfiguration) {
    this.landingZoneAzureRegionConfiguration = landingZoneAzureRegionConfiguration;
  }

  /**
   * Returns a ParametersResolver for a set of input parameters and Azure region. Input parameters
   * take precedence over regional default parameters, which in turn take precedence over default
   * landing zone parameters defined in `LandingZoneDefaultParameters`. If the specified Azure
   * region is not found, it falls back to the global defaults.
   */
  public ParametersResolver create(Map<String, String> inputParameters, String region) {
    var parameters = new HashMap<String, String>();
    parameters.putAll(LandingZoneDefaultParameters.get());

    if (region != null) {
      parameters.putAll(
          landingZoneAzureRegionConfiguration
              .getDefaultParameters()
              .getOrDefault(
                  region,
                  landingZoneAzureRegionConfiguration
                      .getDefaultParameters()
                      .getOrDefault(GLOBAL_KEY, new HashMap<>())));
    } else {
      parameters.putAll(
          landingZoneAzureRegionConfiguration
              .getDefaultParameters()
              .getOrDefault(GLOBAL_KEY, new HashMap<>()));
    }
    if (inputParameters != null) {
      parameters.putAll(inputParameters);
    }

    return new ParametersResolver(parameters);
  }
}
