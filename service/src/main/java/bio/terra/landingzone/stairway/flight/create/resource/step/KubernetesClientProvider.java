package bio.terra.landingzone.stairway.flight.create.resource.step;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import io.kubernetes.client.openapi.apis.CoreV1Api;

public interface KubernetesClientProvider {
  CoreV1Api createCoreApiClient(ArmManagers armManagers, String mrgName, String aksClusterName);
}
