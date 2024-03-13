package bio.terra.landingzone.terraform;

import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import com.hashicorp.cdktf.PgBackend;
import com.hashicorp.cdktf.PgBackendConfig;
import com.hashicorp.cdktf.TerraformStack;
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProvider;
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProviderFeatures;
import com.hashicorp.cdktf.providers.azurerm.storage_account.StorageAccount;
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
      String storageAcctName) {
    super(scope, mrgId);

    new PgBackend(
        this, PgBackendConfig.builder().schemaName("public").connStr(buildPgUri(dbConfig)).build());

    // define resources here
    AzurermProvider.Builder.create(this, "azurerm")
        .features(AzurermProviderFeatures.builder().build())
        .build();

    StorageAccount.Builder.create(this, "storageAccount")
        .name(storageAcctName)
        .resourceGroupName(mrgId)
        .accountReplicationType("LRS")
        .accountTier("Standard")
        .location("eastus")
        .build();
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
