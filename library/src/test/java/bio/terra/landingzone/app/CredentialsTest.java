package bio.terra.landingzone.app;

import bio.terra.landingzone.library.AzureCredentialsProvider;
import bio.terra.landingzone.stairway.flight.create.BaseIntegrationTest;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Tag("integration")
@ActiveProfiles("test")
@PropertySource(value = "classpath:integration_azure_env.properties", ignoreResourceNotFound = true)
@ExtendWith(SpringExtension.class)
@SpringBootTest()
@SpringBootApplication(
    scanBasePackages = {
      "bio.terra.common.logging",
      "bio.terra.common.migrate",
      "bio.terra.common.kubernetes",
      "bio.terra.common.stairway",
      "bio.terra.landingzone"
    },
    exclude = {DataSourceAutoConfiguration.class})
@EnableRetry
@EnableTransactionManagement
public class CredentialsTest extends BaseIntegrationTest {

  @Autowired private AzureCredentialsProvider credentialsProvider;

  @AfterEach
  void cleanUpResources() {}

  @Test
  void doSomething() throws InterruptedException {
    var tokenCredential = credentialsProvider.getTokenCredential();
    var start = DateTime.now();
    var expiration = start.plusMinutes(10);
    System.out.println("********** POLLING UNTIL " + expiration.toString());
    while (DateTime.now().isBefore(expiration)) {
      System.out.println(
          "********** GETTING A FRESH TOKEN, HAVE BEEN RUNNING FOR "
              + Seconds.secondsBetween(DateTime.now(), start)
              + " SECONDS");
      AccessToken token =
          tokenCredential.getTokenSync(
              new TokenRequestContext()
                  .setTenantId(azureProfile.getTenantId())
                  .setScopes(List.of("https://management.core.windows.net/.default")));

      System.out.println("********** GOT A TOKEN, SLEEPING FOR 10 SECONDS");
      Thread.sleep(10000);
    }
  }
}
