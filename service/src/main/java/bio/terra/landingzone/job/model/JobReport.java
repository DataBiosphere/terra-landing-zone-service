package bio.terra.landingzone.job.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public class JobReport {
  private String id = null;

  private String description = null;

  /** status of the job */
  public enum StatusEnum {
    RUNNING("RUNNING"),

    SUCCEEDED("SUCCEEDED"),

    FAILED("FAILED");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String text) {
      for (StatusEnum b : StatusEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      throw new IllegalArgumentException(
          "Unexpected value '" + text + "' for 'ApiJobReport' enum.");
    }
  }

  @JsonProperty("status")
  private StatusEnum status = null;

  @JsonProperty("statusCode")
  private Integer statusCode = null;

  @JsonProperty("submitted")
  private String submitted = null;

  @JsonProperty("completed")
  private String completed = null;

  @JsonProperty("resultURL")
  private String resultURL = null;

  public JobReport id(String id) {
    this.id = id;
    return this;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public JobReport description(String description) {
    this.description = description;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public JobReport status(StatusEnum status) {
    this.status = status;
    return this;
  }

  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public JobReport statusCode(Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public JobReport submitted(String submitted) {
    this.submitted = submitted;
    return this;
  }

  public String getSubmitted() {
    return submitted;
  }

  public void setSubmitted(String submitted) {
    this.submitted = submitted;
  }

  public JobReport completed(String completed) {
    this.completed = completed;
    return this;
  }

  public String getCompleted() {
    return completed;
  }

  public void setCompleted(String completed) {
    this.completed = completed;
  }

  public JobReport resultURL(String resultURL) {
    this.resultURL = resultURL;
    return this;
  }

  public String getResultURL() {
    return resultURL;
  }

  public void setResultURL(String resultURL) {
    this.resultURL = resultURL;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobReport jobReport = (JobReport) o;
    return Objects.equals(this.id, jobReport.id)
        && Objects.equals(this.description, jobReport.description)
        && Objects.equals(this.status, jobReport.status)
        && Objects.equals(this.statusCode, jobReport.statusCode)
        && Objects.equals(this.submitted, jobReport.submitted)
        && Objects.equals(this.completed, jobReport.completed)
        && Objects.equals(this.resultURL, jobReport.resultURL);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, description, status, statusCode, submitted, completed, resultURL);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiJobReport {\n");

    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
    sb.append("    submitted: ").append(toIndentedString(submitted)).append("\n");
    sb.append("    completed: ").append(toIndentedString(completed)).append("\n");
    sb.append("    resultURL: ").append(toIndentedString(resultURL)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
