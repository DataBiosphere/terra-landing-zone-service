package bio.terra.landingzone.terraform;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.db.model.LandingZoneRecord;
import bio.terra.landingzone.library.AzureCredentialsProvider;
import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.library.landingzones.management.LandingZoneManager;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.terraform.client.TerraformClient;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.hashicorp.cdktf.App;
import com.hashicorp.cdktf.AppConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TerraformService {

  private final LandingZoneDatabaseConfiguration landingZoneDatabaseConfiguration;
  private final LandingZoneService landingZoneService;
  private final LandingZoneDao landingZoneDao;
  private final AzureCredentialsProvider azureCredentialsProvider;

  private final Logger logger = LoggerFactory.getLogger(TerraformService.class);

  @Autowired
  public TerraformService(
      LandingZoneDatabaseConfiguration landingZoneDatabaseConfiguration,
      LandingZoneService landingZoneService,
      LandingZoneDao landingZoneDao,
      AzureCredentialsProvider azureCredentialsProvider) {
    this.landingZoneDatabaseConfiguration = landingZoneDatabaseConfiguration;
    this.landingZoneService = landingZoneService;
    this.landingZoneDao = landingZoneDao;
    this.azureCredentialsProvider = azureCredentialsProvider;
  }

  public String terraformPlan(UUID landingZoneId, BearerToken bearerToken) {
    // ensure access
    landingZoneService.getLandingZone(bearerToken, landingZoneId);
    var record = landingZoneDao.getLandingZoneRecord(landingZoneId);
    var realAcct = getLandingZoneStorageAccount(record, bearerToken);

    try {
      var tmpdir = Files.createTempDirectory("lz-terraform").toString();
      synthesize(tmpdir, record.resourceGroupId(), realAcct);
      return doPlan(tmpdir, record.resourceGroupId());
    } catch (IOException e) {
      logger.error("Error creating temp directory for landing zone Terraform", e);
      throw new LandingZoneTerraformException(
          "Error creating temp directory for landing zone Terraform", e);
    }
  }

  // TODO return a richer object
  private String doPlan(String tmpdir, String mrgId) {
    try (TerraformClient cf = new TerraformClient()) {
      var output = new ArrayList<String>();
      Consumer<String> outputConsumer = output::add;

      logger.info("Running terraform plan...");
      cf.setWorkingDirectory(new File(tmpdir + "/stacks/" + mrgId));
      cf.setOutputListener(outputConsumer);
      cf.setErrorListener(outputConsumer);

      cf.plan().get();
      var result = output.stream().reduce("", (a, b) -> a + "\n" + b);
      logger.debug(result);
      return result;
    } catch (Exception e) {
      logger.error("Error running terraform plan", e);
      throw new LandingZoneTerraformException("Error running terraform plan", e);
    }
  }

  private void synthesize(String tmpdir, String mrgId, StorageAccount realAcct) {
    logger.info("Synthesizing terraform...");
    AppConfig appConfig = AppConfig.builder().outdir(tmpdir).build();
    var app = new App(appConfig);

    new LandingZoneStack(app, mrgId, landingZoneDatabaseConfiguration, realAcct);
    app.synth();
  }

  private StorageAccount getLandingZoneStorageAccount(
      LandingZoneRecord record, BearerToken bearerToken) {
    var resources =
        landingZoneService.listResourcesByPurpose(
            bearerToken, record.landingZoneId(), ResourcePurpose.SHARED_RESOURCE);
    var accts =
        resources.stream()
            .filter(r -> r.resourceType().equals("Microsoft.Storage/storageAccounts"))
            .toList();
    if (accts.size() != 1) {
      throw new RuntimeException("Expected 1 storage account, found " + accts.size());
    }

    var azureProfile =
        new AzureProfile(record.tenantId(), record.subscriptionId(), AzureEnvironment.AZURE);
    var armManagers =
        LandingZoneManager.createArmManagers(
            azureCredentialsProvider.getTokenCredential(), azureProfile, "ignore");

    return armManagers.azureResourceManager().storageAccounts().getById(accts.get(0).resourceId());
  }
}
