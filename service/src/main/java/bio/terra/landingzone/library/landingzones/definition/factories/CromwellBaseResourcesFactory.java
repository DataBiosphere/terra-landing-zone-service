package bio.terra.landingzone.library.landingzones.definition.factories;

import bio.terra.landingzone.library.landingzones.definition.ArmManagers;
import bio.terra.landingzone.library.landingzones.definition.DefinitionHeader;
import bio.terra.landingzone.library.landingzones.definition.DefinitionVersion;
import bio.terra.landingzone.library.landingzones.definition.LandingZoneDefinable;
import java.util.List;

/**
 * An implementation of {@link LandingZoneDefinitionFactory} that deploys resources required for
 * cromwell. Current resources are: - VNet: Subnets required for AKS, Batch, PostgreSQL and
 * Compute/VMs - AKS Account (?) TODO - AKS Nodepool TODO - Batch Account TODO - Storage Account
 * TODO - PostgreSQL server TODO
 */
public class CromwellBaseResourcesFactory extends ArmClientsDefinitionFactory {
  private final String LZ_NAME = "Cromwell Landing Zone Base Resources";
  private final String LZ_DESC =
      "Cromwell Base Resources: VNet, AKS Account & Nodepool, Batch Account,"
          + " Storage Account, PostgreSQL server, Subnets for AKS, Batch, Posgres, and Compute";

  public static final String STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_ORIGINS_DEFAULT = "*";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_METHODS_DEFAULT =
      "GET,HEAD,OPTIONS,PUT,PATCH,POST,MERGE,DELETE";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_ALLOWED_HEADERS_DEFAULT =
      "authorization,content-type,x-app-id,Referer,x-ms-blob-type,x-ms-copy-source,content-length";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_EXPOSED_HEADERS_DEFAULT = "";
  public static final String STORAGE_ACCOUNT_BLOB_CORS_MAX_AGE_DEFAULT = "0";

  public enum Subnet {
    AKS_SUBNET,
    BATCH_SUBNET,
    POSTGRESQL_SUBNET,
    COMPUTE_SUBNET
  }

  public enum ParametersNames {
    POSTGRES_DB_ADMIN,
    POSTGRES_DB_PASSWORD,
    POSTGRES_SERVER_SKU,
    VNET_ADDRESS_SPACE,
    AUDIT_LOG_RETENTION_DAYS,
    AKS_NODE_COUNT,
    AKS_MACHINE_TYPE,
    AKS_AUTOSCALING_ENABLED,
    AKS_AUTOSCALING_MIN,
    AKS_AUTOSCALING_MAX
  }

  CromwellBaseResourcesFactory() {}

  public CromwellBaseResourcesFactory(ArmManagers armManagers) {
    super(armManagers);
  }

  @Override
  public DefinitionHeader header() {
    return new DefinitionHeader(LZ_NAME, LZ_DESC);
  }

  @Override
  public List<DefinitionVersion> availableVersions() {
    return List.of(DefinitionVersion.V1);
  }

  @Override
  public LandingZoneDefinable create(DefinitionVersion version) {
    if (version.equals(DefinitionVersion.V1)) {
      return new CromwellBaseResourcesDefinitionV1(armManagers);
    }
    throw new RuntimeException("Invalid Version");
  }
}
