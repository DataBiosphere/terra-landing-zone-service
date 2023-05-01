package bio.terra.landingzone.stairway.flight;

import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// TODO: spread parameters among flight steps
public class LandingZoneDefaultParameters {
  private LandingZoneDefaultParameters() {}

  public static Map<String, String> get() {
    Map<String, String> defaultValues = new HashMap<>();
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_ADMIN.name(), "db_admin");
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_DB_PASSWORD.name(),
        UUID.randomUUID().toString());
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_SKU.name(), "GP_Gen5_2");
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.VNET_ADDRESS_SPACE.name(), "10.1.0.0/27");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name(), "10.1.0.0/29");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name(), "10.1.0.8/29");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name(), "10.1.0.16/29");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name(), "10.1.0.24/29");
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_NODE_COUNT.name(), String.valueOf(1));
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_MACHINE_TYPE.name(),
        ContainerServiceVMSizeTypes.STANDARD_A2_V2.toString());
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_ENABLED.name(),
        String.valueOf(false));
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MIN.name(), String.valueOf(1));
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_AUTOSCALING_MAX.name(), String.valueOf(3));
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AUDIT_LOG_RETENTION_DAYS.name(), "90");
    defaultValues.put(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT);
    defaultValues.put(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT);
    defaultValues.put(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT);
    defaultValues.put(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS.name(),
        STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT);
    defaultValues.put(
        StorageAccountBlobCorsParametersNames.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE.name(),
        STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT);
    return defaultValues;
  }
}
