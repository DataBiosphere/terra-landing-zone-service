package bio.terra.landingzone.terraform;

import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import bio.terra.landingzone.terraform.updaters.AKSUpdater;
import bio.terra.landingzone.terraform.updaters.StorageAccountUpdater;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.hashicorp.cdktf.PgBackend;
import com.hashicorp.cdktf.PgBackendConfig;
import com.hashicorp.cdktf.TerraformStack;
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProvider;
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProviderFeatures;
import software.constructs.Construct;

/**
 * Rudimentary Terraform stack for a landing zone.
 * https://developer.hashicorp.com/terraform/cdktf/concepts/stacks
 */
public class LandingZoneStack extends TerraformStack {

  public LandingZoneStack(
      Construct scope,
      String mrgId,
      LandingZoneDatabaseConfiguration dbConfig,
      com.azure.resourcemanager.storage.models.StorageAccount existingStorageAccount,
      KubernetesCluster existingAksCluster) {
    super(scope, mrgId);

    new PgBackend(
        this, PgBackendConfig.builder().schemaName("public").connStr(buildPgUri(dbConfig)).build());

    // define resources here
    AzurermProvider.Builder.create(this, "azurerm")
        .features(AzurermProviderFeatures.builder().build())
        .build();

    new StorageAccountUpdater(this, existingStorageAccount)
        .updateResource(existingStorageAccount.name(), existingStorageAccount.resourceGroupName());

    new AKSUpdater(this).updateResource(existingAksCluster.name(), existingAksCluster);
  }

  private String buildPgUri(LandingZoneDatabaseConfiguration dbConfig) {
    return dbConfig
            .getUri()
            .replace(
                "jdbc:postgresql://",
                "postgres://" + dbConfig.getUsername() + ":" + dbConfig.getPassword() + "@")
        + "?sslmode=disable";
  }
}
