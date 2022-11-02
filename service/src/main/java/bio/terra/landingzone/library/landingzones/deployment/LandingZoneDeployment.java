package bio.terra.landingzone.library.landingzones.deployment;

import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.loganalytics.models.Workspace;
import com.azure.resourcemanager.monitor.models.DiagnosticSetting;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.PrivateEndpoint;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.relay.models.RelayNamespace;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource.DefinitionWithTags;
import com.azure.resourcemanager.resources.fluentcore.model.Creatable;
import java.util.List;
import reactor.core.publisher.Flux;

/** Fluent API to define resource purpose and deployment in a landing zone. */
public interface LandingZoneDeployment {

  interface FluentDefinition
      extends DefinitionStages.WithLandingZoneResource, DefinitionStages.Deployable {}

  interface DefinitionStages {
    interface Definable {
      WithLandingZoneResource define(String landingZoneId);
    }

    interface WithLandingZoneResource {
      <T extends Creatable<?> & DefinitionWithTags<?>> Deployable withResourceWithPurpose(
          T resource, ResourcePurpose purpose);

      <T extends Creatable<?>> Deployable withResource(T resource);

      Deployable withVNetWithPurpose(
          Network.DefinitionStages.WithCreateAndSubnet virtualNetwork,
          String subnetName,
          SubnetResourcePurpose purpose);

      Deployable withResourceWithPurpose(
          RelayNamespace.DefinitionStages.WithCreate relay, ResourcePurpose sharedResource);

      Deployable withResourceWithPurpose(
          BatchAccount.DefinitionStages.WithCreate batchAccount, ResourcePurpose sharedResource);

      Deployable withResourceWithPurpose(
          Server.DefinitionStages.WithCreate batchAccount, ResourcePurpose sharedResource);

      Deployable withResourceWithPurpose(
          PrivateEndpoint.DefinitionStages.WithCreate privateEndpoint,
          ResourcePurpose sharedResource);

      Deployable withResourceWithPurpose(
          Workspace.DefinitionStages.WithCreate logAnalyticsWorkspace,
          ResourcePurpose sharedResource);

      Deployable withResourceWithPurpose(
          DiagnosticSetting.DefinitionStages.WithCreate logAnalyticsWorkspace,
          ResourcePurpose sharedResource);

      WithLandingZoneResource definePrerequisites();
    }

    interface Deployable extends WithLandingZoneResource {
      List<DeployedResource> deploy();

      Flux<DeployedResource> deployAsync();
    }
  }
}
