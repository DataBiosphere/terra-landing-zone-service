package bio.terra.landingzone.stairway.flight;

import bio.terra.landingzone.library.configuration.LandingZoneAzureRegionConfiguration;
import bio.terra.landingzone.library.landingzones.definition.factories.ParametersResolver;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParametersResolverProvider {

  private final LandingZoneAzureRegionConfiguration landingZoneAzureRegionConfiguration;

  @Autowired
  public ParametersResolverProvider(
      LandingZoneAzureRegionConfiguration landingZoneAzureRegionConfiguration) {
    this.landingZoneAzureRegionConfiguration = landingZoneAzureRegionConfiguration;
  }

  /**
   * Returns a ParametersResolver for a set of input parameters and Azure region. Input parameters
   * take precedence over regional default parameters, which in turn take precedence over default
   * landing zone parameters defined in `LandingZoneDefaultParameters`.
   */
  public ParametersResolver create(Map<String, String> inputParameters, String region) {
    var parameters = new HashMap<>(LandingZoneDefaultParameters.get());

    var regionalParameters = landingZoneAzureRegionConfiguration.getDefaultParameters();

    if (regionalParameters != null) {
      if (region == null) {
        throw new IllegalArgumentException("Region must not be null.");
      } else {
        parameters.putAll(regionalParameters.getOrDefault(region, new HashMap<>()));
      }
    }
    if (inputParameters != null) {
      parameters.putAll(inputParameters);
    }

    return new ParametersResolver(parameters);
  }
}
