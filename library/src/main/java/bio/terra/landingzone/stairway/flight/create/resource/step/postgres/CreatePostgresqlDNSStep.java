package bio.terra.landingzone.stairway.flight.create.resource.step.postgres;

import bio.terra.landingzone.common.utils.LandingZoneFlightBeanBag;
import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.ResourceNameGenerator;
import bio.terra.landingzone.library.landingzones.deployment.LandingZoneTagKeys;
import bio.terra.landingzone.service.landingzone.azure.model.LandingZoneResource;
import bio.terra.landingzone.stairway.flight.LandingZoneFlightMapKeys;
import bio.terra.landingzone.stairway.flight.ResourceNameProvider;
import bio.terra.landingzone.stairway.flight.ResourceNameRequirements;
import bio.terra.landingzone.stairway.flight.create.resource.step.BaseResourceCreateStep;
import bio.terra.stairway.FlightContext;
import com.azure.core.management.AzureEnvironment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePostgresqlDNSStep extends BaseResourceCreateStep {
  private static final Logger logger = LoggerFactory.getLogger(CreatePostgresqlDNSStep.class);
  public static final String POSTGRESQL_DNS_ID = "POSTGRESQL_DNS_ID";
  public static final String POSTGRESQL_DNS_RESOURCE_KEY = "POSTGRESQL_DNS";
  public static final String POSTGRES_DNS_SUFFIX = ".private.postgres.database.azure.com";
  public static final String POSTGRES_DNS_SUFFIX_GOV =
      ".private.postgres.database.usgovcloudapi.net";

  public CreatePostgresqlDNSStep(
      ArmManagers armManagers, ResourceNameProvider resourceNameProvider) {
    super(armManagers, resourceNameProvider);
  }

  @Override
  protected void createResource(FlightContext context, ArmManagers armManagers) {
    var beanBag = LandingZoneFlightBeanBag.getFromObject(context.getApplicationContext());
    var azureEnvironment = beanBag.getAzureConfiguration().getAzureEnvironment();

    var landingZoneId =
        getParameterOrThrow(
            context.getInputParameters(), LandingZoneFlightMapKeys.LANDING_ZONE_ID, UUID.class);

    var dnsZoneName = resourceNameProvider.getName(getResourceType());

    String postgresDnsSuffixForEnvironment =
        azureEnvironment == AzureEnvironment.AZURE_US_GOVERNMENT
            ? POSTGRES_DNS_SUFFIX_GOV
            : POSTGRES_DNS_SUFFIX;

    var dns =
        armManagers
            .azureResourceManager()
            .privateDnsZones()
            .define(dnsZoneName + postgresDnsSuffixForEnvironment)
            .withExistingResourceGroup(getMRGName(context))
            .withTags(
                Map.of(LandingZoneTagKeys.LANDING_ZONE_ID.toString(), landingZoneId.toString()))
            .create();

    context.getWorkingMap().put(POSTGRESQL_DNS_ID, dns.id());
    context
        .getWorkingMap()
        .put(
            POSTGRESQL_DNS_RESOURCE_KEY,
            LandingZoneResource.builder()
                .resourceId(dns.id())
                .resourceType(dns.type())
                .tags(dns.tags())
                .region(dns.regionName())
                .resourceName(dns.name())
                .build());
    logger.info(RESOURCE_CREATED, getResourceType(), dns.id(), getMRGName(context));
  }

  @Override
  protected void deleteResource(String resourceId) {
    armManagers.azureResourceManager().privateDnsZones().deleteById(resourceId);
  }

  @Override
  protected String getResourceType() {
    return "PrivateDnsZone";
  }

  @Override
  protected Optional<String> getResourceId(FlightContext context) {
    return Optional.ofNullable(context.getWorkingMap().get(POSTGRESQL_DNS_ID, String.class));
  }

  @Override
  public List<ResourceNameRequirements> getResourceNameRequirements() {
    return List.of(
        new ResourceNameRequirements(
            getResourceType(), ResourceNameGenerator.MAX_PRIVATE_DNS_ZONE_NAME_LENGTH));
  }
}
