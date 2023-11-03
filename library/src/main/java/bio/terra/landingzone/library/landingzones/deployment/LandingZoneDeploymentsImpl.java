package bio.terra.landingzone.library.landingzones.deployment;

import bio.terra.landingzone.library.landingzones.deployment.LandingZoneDeployment.DefinitionStages.WithLandingZoneResource;
import com.azure.core.util.logging.ClientLogger;
import org.apache.commons.lang3.StringUtils;

/** Implementation of {@link LandingZoneDeployments} */
public class LandingZoneDeploymentsImpl implements LandingZoneDeployments {
  private final ClientLogger logger = new ClientLogger(LandingZoneDeploymentsImpl.class);

  @Override
  public WithLandingZoneResource define(String landingZoneId) {

    if (StringUtils.isBlank(landingZoneId)) {
      throw logger.logExceptionAsError(
          new IllegalArgumentException("Invalid landing zone id. It can't be blank or null"));
    }

    return new LandingZoneDeploymentImpl(new ResourcesTagMapWrapper(landingZoneId));
  }
}
