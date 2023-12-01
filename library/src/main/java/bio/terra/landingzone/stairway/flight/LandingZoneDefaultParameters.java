package bio.terra.landingzone.stairway.flight;

import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT;
import static bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory.STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT;

import bio.terra.landingzone.library.landingzones.definition.factories.CromwellBaseResourcesFactory;
import bio.terra.landingzone.library.landingzones.definition.factories.parameters.StorageAccountBlobCorsParametersNames;
import com.azure.resourcemanager.containerservice.models.ContainerServiceVMSizeTypes;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerVersion;
import com.azure.resourcemanager.postgresqlflexibleserver.models.SkuTier;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_SKU.name(),
        "Standard_D2ds_v4");
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_SKU_TIER.name(),
        SkuTier.GENERAL_PURPOSE.toString());
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_VERSION.name(),
        ServerVersion.ONE_FOUR.toString());
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_BACKUP_RETENTION_DAYS.name(),
        "28");
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.POSTGRES_SERVER_STORAGE_SIZE_GB.name(), "128");

    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.VNET_ADDRESS_SPACE.name(), "10.1.0.0/27");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.AKS_SUBNET.name(), "10.1.0.0/29");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.BATCH_SUBNET.name(), "10.1.0.8/29");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.POSTGRESQL_SUBNET.name(), "10.1.0.16/29");
    defaultValues.put(CromwellBaseResourcesFactory.Subnet.COMPUTE_SUBNET.name(), "10.1.0.24/29");
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_NODE_COUNT.name(), String.valueOf(1));
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_SPOT_AUTOSCALING_MAX.name(),
        String.valueOf(10));
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
        CromwellBaseResourcesFactory.ParametersNames.AKS_COST_SAVING_SPOT_NODES_ENABLED.name(),
        String.valueOf(false));
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_COST_SAVING_VPA_ENABLED.name(),
        String.valueOf(false));
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
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.STORAGE_ACCOUNT_SKU_TYPE.name(),
        StorageAccountSkuType.STANDARD_LRS.name().toString());
    defaultValues.put(
        CromwellBaseResourcesFactory.ParametersNames.AKS_AAD_PROFILE_USER_GROUP_ID.name(),
        "00000000-0000-0000-0000-000000000000");
    defaultValues.put(CromwellBaseResourcesFactory.ParametersNames.ENABLE_PGBOUNCER.name(), "true");
    return defaultValues;
  }
}
