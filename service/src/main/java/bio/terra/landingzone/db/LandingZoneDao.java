package bio.terra.landingzone.db;

import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.landingzone.db.exception.DuplicateLandingZoneException;
import bio.terra.landingzone.db.exception.LandingZoneNotFoundException;
import bio.terra.landingzone.db.model.LandingZone;
import bio.terra.landingzone.library.configuration.LandingZoneDatabaseConfiguration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** LandingZoneDao includes operations on the landing zone tables. */
@Component
public class LandingZoneDao {
  /** SQL query for reading landing zone records. */
  private static final String LANDINGZONE_SELECT_SQL =
      "SELECT landingzone_id, resource_group, subscription_id, tenant_id, definition_id, definition_version_id, display_name, description, properties"
          + " FROM landingzone";

  private final Logger logger = LoggerFactory.getLogger(LandingZoneDao.class);
  private final LandingZoneDatabaseConfiguration landingZoneDatabaseConfiguration;
  private final NamedParameterJdbcTemplate jdbcLandingZoneTemplate;

  @Autowired
  public LandingZoneDao(LandingZoneDatabaseConfiguration landingZoneDatabaseConfiguration) {
    this.landingZoneDatabaseConfiguration = landingZoneDatabaseConfiguration;
    this.jdbcLandingZoneTemplate =
        new NamedParameterJdbcTemplate(landingZoneDatabaseConfiguration.getDataSource());
  }

  /**
   * Persists a landing zone record to DB. Returns ID of persisted landing zone on success.
   *
   * @param landingzone all properties of the landing zone to create
   * @return landingzone id
   */
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tlzTransactionManager")
  public UUID createLandingZone(LandingZone landingzone) {
    final String sql =
        "INSERT INTO landingzone (landingzone_id, resource_group, subscription_id, tenant_id, definition_id, definition_version_id, display_name, description, properties) "
            + "values (:landingzone_id, :resource_group, :subscription_id, :tenant_id, :definition_id, :definition_version_id, :display_name, :description,"
            + " cast(:properties AS jsonb))";

    final String landingZoneUuid = landingzone.landingZoneId().toString();

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("landingzone_id", landingZoneUuid)
            .addValue("resource_group", landingzone.resourceGroupId())
            .addValue("subscription_id", landingzone.subscriptionId())
            .addValue("tenant_id", landingzone.tenantId())
            .addValue("definition_id", landingzone.definition())
            .addValue("definition_version_id", landingzone.version())
            .addValue("display_name", landingzone.displayName().orElse(null))
            .addValue("description", landingzone.description().orElse(null))
            .addValue("properties", DbSerDes.propertiesToJson(landingzone.properties()));
    try {
      jdbcLandingZoneTemplate.update(sql, params);
      logger.info("Inserted record for landing zone {}", landingZoneUuid);
    } catch (DuplicateKeyException e) {
      if (e.getMessage()
          .contains("duplicate key value violates unique constraint \"landingzone_pkey\"")) {
        // Landing Zone record with landingzone_id already exists.
        throw new DuplicateLandingZoneException(
            String.format(
                "Landing Zone with id %s already exists - display name %s definition %s version %s",
                landingZoneUuid,
                landingzone.displayName().toString(),
                landingzone.definition(),
                landingzone.version()),
            e);
      } else {
        throw e;
      }
    }
    return landingzone.landingZoneId();
  }

  /**
   * @param landingZoneUuid unique identifier of the landing zone
   * @return true on successful delete, false if there's nothing to delete
   */
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tlzTransactionManager")
  public boolean deleteLandingZone(UUID landingZoneUuid) {
    final String sql = "DELETE FROM landingzone WHERE landingzone_id = :id";

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("id", landingZoneUuid.toString());
    int rowsAffected = jdbcLandingZoneTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;

    if (deleted) {
      logger.info("Deleted record for landing zone {}", landingZoneUuid);
    } else {
      logger.info("No record found for delete landing zone {}", landingZoneUuid);
    }

    return deleted;
  }

  /**
   * Retrieves a landing zone from database by ID.
   *
   * @param uuid unique identifier of the landing zone
   * @return landing zone value object
   */
  public LandingZone getLandingZone(UUID uuid) {
    return getLandingZoneIfExists(uuid)
        .orElseThrow(
            () ->
                new LandingZoneNotFoundException(
                    String.format("Landing Zone %s not found.", uuid.toString())));
  }

  /**
   * Retrieves landing zones from database by subscription ID, tenant ID, and resource group ID .
   *
   * @param subscriptionId unique identifier of the Azure subscription.
   * @param tenantId unique identifier of the Azure tenant.
   * @param resourceGroupId unique identifier of the Azure resource group.
   * @return List of landing zone objects.
   */
  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tlzTransactionManager")
  public List<LandingZone> getLandingZoneList(
      String subscriptionId, String tenantId, String resourceGroupId) {
    if (subscriptionId == null || subscriptionId.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Subscription ID cannot be null or empty.", subscriptionId));
    }
    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Tenant ID cannot be null or empty.", tenantId));
    }
    if (resourceGroupId == null || resourceGroupId.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Resource Group ID cannot be null or empty.", resourceGroupId));
    }

    String sql =
        LANDINGZONE_SELECT_SQL
            + " WHERE subscription_id = :subscription_id"
            + " AND tenant_id = :tenant_id"
            + " AND resource_group = :resource_group_id";

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("subscription_id", subscriptionId)
            .addValue("tenant_id", tenantId)
            .addValue("resource_group_id", resourceGroupId);

    return jdbcLandingZoneTemplate.query(sql, params, LANDINGZONE_ROW_MAPPER);
  }

  @Transactional(
      isolation = Isolation.SERIALIZABLE,
      propagation = Propagation.REQUIRED,
      transactionManager = "tlzTransactionManager")
  public Optional<LandingZone> getLandingZoneIfExists(UUID uuid) {
    if (uuid == null) {
      throw new MissingRequiredFieldException("Valid landing zone id is required");
    }
    String sql = LANDINGZONE_SELECT_SQL + " WHERE landingzone_id = :id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", uuid.toString());
    try {
      LandingZone result =
          DataAccessUtils.requiredSingleResult(
              jdbcLandingZoneTemplate.query(sql, params, LANDINGZONE_ROW_MAPPER));
      logger.info("Retrieved landing zone record {}", result);
      return Optional.of(result);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  private static final RowMapper<LandingZone> LANDINGZONE_ROW_MAPPER =
      (rs, rowNum) ->
          LandingZone.builder()
              .landingZoneId(UUID.fromString(rs.getString("landingzone_id")))
              .resourceGroupId(rs.getString("resource_group"))
              .subscriptionId(rs.getString("subscription_id"))
              .tenantId(rs.getString("tenant_id"))
              .definition(rs.getString("definition_id"))
              .version(rs.getString("definition_version_id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .properties(
                  Optional.ofNullable(rs.getString("properties"))
                      .map(DbSerDes::jsonToProperties)
                      .orElse(null))
              .build();
}
