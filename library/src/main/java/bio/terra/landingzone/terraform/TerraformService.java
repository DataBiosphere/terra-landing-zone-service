package bio.terra.landingzone.terraform;

import bio.terra.common.iam.BearerToken;
import bio.terra.landingzone.db.LandingZoneDao;
import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import bio.terra.landingzone.library.landingzones.deployment.ResourcePurpose;
import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.terraform.client.TerraformClient;
import com.hashicorp.cdktf.App;
import com.hashicorp.cdktf.AppConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
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
  private final Logger logger = LoggerFactory.getLogger(TerraformService.class);

  @Autowired
  public TerraformService(
      LandingZoneDatabaseConfiguration landingZoneDatabaseConfiguration,
      LandingZoneService landingZoneService,
      LandingZoneDao landingZoneDao) {
    this.landingZoneDatabaseConfiguration = landingZoneDatabaseConfiguration;
    this.landingZoneService = landingZoneService;
    this.landingZoneDao = landingZoneDao;
  }

  public String terraformPlan(UUID landingZoneId, BearerToken bearerToken) {
    // ensure access
    landingZoneService.getLandingZone(bearerToken, landingZoneId);
    var record = landingZoneDao.getLandingZoneRecord(landingZoneId);

    var acct = getLandingZoneStorageAccount(landingZoneId, bearerToken);

    // TODO how does this hold up with concurrent requests?
    try (TerraformClient cf = new TerraformClient()) {
      logger.warn("Synthesizing terraform...");
      String tmpdir = Files.createTempDirectory("tftmp").toFile().getAbsolutePath();
      AppConfig appConfig = AppConfig.builder().outdir(tmpdir).build();

      var app = new App(appConfig);
      var mrgId = record.resourceGroupId();
      new LandingZoneStack(app, mrgId, landingZoneDatabaseConfiguration, acct.resourceName().get());
      app.synth();

      var output = new ArrayList<String>();
      Consumer<String> outputConsumer = output::add;

      logger.info("Running terraform plan...");
      cf.setWorkingDirectory(new File(tmpdir + "/stacks/" + mrgId));
      cf.setOutputListener(outputConsumer);

      // dump the output to a string
      // TODO transform to JSON?
      var planner = cf.plan();
      planner.get();
      return output.stream().reduce("", (a, b) -> a + "\n" + b);;

      //      TODO This is how an apply would work
      //      logger.info("Applying terraform...");
      //      var applyer = cf.apply();
      //      applyer.get();
      //      var applyOut = output.stream().reduce("", (a, b) -> a + "\n" + b);
      //      logger.info(applyOut);

    } catch (IOException | InterruptedException | ExecutionException e) {
      logger.error("Error running terraform plan", e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return "";
  }

  private LandingZoneResource getLandingZoneStorageAccount(UUID landingZoneId, BearerToken bearerToken) {
    var resources =
        landingZoneService.listResourcesByPurpose(
                bearerToken, landingZoneId, ResourcePurpose.SHARED_RESOURCE);
    var accts =
        resources.stream()
            .filter(r -> r.resourceType().equals("Microsoft.Storage/storageAccounts"))
            .toList();
    if (accts.size() != 1) {
      throw new RuntimeException("Expected 1 storage account, found " + accts.size());
    }
    var acct = accts.get(0);
    return acct;
  }
}
