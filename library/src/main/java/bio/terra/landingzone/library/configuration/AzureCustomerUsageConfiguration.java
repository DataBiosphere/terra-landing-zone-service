package bio.terra.landingzone.library.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "azure.customer")
public class AzureCustomerUsageConfiguration {
  private String usageAttribute;

  public String getUsageAttribute() {
    return usageAttribute;
  }

  public void setUsageAttribute(String usageAttribute) {
    this.usageAttribute = usageAttribute;
  }
}
