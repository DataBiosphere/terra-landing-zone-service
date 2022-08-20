package bio.terra.landingzone.job.model;

public enum OperationType {
  CREATE("CREATE"),
  UNKNOWN("UNKNOWN");

  private final String operationType;

  OperationType(String operationType) {
    this.operationType = operationType;
  }
}
