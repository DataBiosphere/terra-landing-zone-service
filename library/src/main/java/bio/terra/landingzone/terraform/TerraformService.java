package bio.terra.landingzone.terraform;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.library.AzureCredentialsProvider;
import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.terraform.client.TerraformClient;
import com.azure.resourcemanager.containerservice.models.KubernetesCluster;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.hashicorp.cdktf.App;
import com.hashicorp.cdktf.AppConfig;
import java.io.File;
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
    var resourceResolver = new ResourceResolver(landingZoneService, azureCredentialsProvider);
    var realAcct = resourceResolver.getLandingZoneStorageAccount(record, bearerToken);
    var realAks = resourceResolver.getLandingZoneAks(record, bearerToken);

    try {
      var tmpdir = Files.createTempDirectory("lz-terraform").toString();
      synthesize(tmpdir, record.resourceGroupId(), realAcct, realAks);
      var result = doPlan(tmpdir, record.resourceGroupId());
      logger.info(result);
      return result;
    } catch (Exception e) {
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

  private void synthesize(
      String tmpdir, String mrgId, StorageAccount realAcct, KubernetesCluster realAks) {
    logger.info("Synthesizing terraform...");
    AppConfig appConfig = AppConfig.builder().outdir(tmpdir).build();
    var app = new App(appConfig);

    new LandingZoneStack(app, mrgId, landingZoneDatabaseConfiguration, realAcct, realAks);
    app.synth();
  }
}
