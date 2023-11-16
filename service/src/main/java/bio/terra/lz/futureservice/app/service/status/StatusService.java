package bio.terra.lz.futureservice.app.service.status;

import bio.terra.lz.futureservice.app.configuration.StatusCheckConfiguration;
import bio.terra.lz.futureservice.generated.model.ApiSystemStatusSystems;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StatusService extends BaseStatusService {
  private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final SamStatusService samStatusService;

  @Autowired
  public StatusService(
      SamStatusService samStatusService,
      NamedParameterJdbcTemplate jdbcTemplate,
      StatusCheckConfiguration configuration) {
    super(configuration);
    this.samStatusService = samStatusService;
    this.jdbcTemplate = jdbcTemplate;
    registerStatusCheck("Database", this::databaseStatus);
    registerStatusCheck("Sam", samStatusService::status);
  }

  private ApiSystemStatusSystems databaseStatus() {
    try {
      logger.debug("Checking database connection valid");
      return new ApiSystemStatusSystems()
          .ok(jdbcTemplate.getJdbcTemplate().execute((Connection conn) -> conn.isValid(5000)));
    } catch (Exception ex) {
      String errorMsg = "Database status check failed";
      logger.error(errorMsg, ex);
      return new ApiSystemStatusSystems()
          .ok(false)
          .addMessagesItem(errorMsg + ": " + ex.getMessage());
    }
  }
}
