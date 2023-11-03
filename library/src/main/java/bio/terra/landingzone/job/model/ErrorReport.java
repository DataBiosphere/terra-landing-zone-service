package bio.terra.landingzone.job.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ErrorReport {
  private String message = null;

  private Integer statusCode = null;

  private List<String> causes = new ArrayList<>();

  public ErrorReport message(String message) {
    this.message = message;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ErrorReport statusCode(Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public ErrorReport causes(List<String> causes) {
    this.causes = causes;
    return this;
  }

  public ErrorReport addCausesItem(String causesItem) {
    this.causes.add(causesItem);
    return this;
  }

  public List<String> getCauses() {
    return causes;
  }

  public void setCauses(List<String> causes) {
    this.causes = causes;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorReport errorReport = (ErrorReport) o;
    return Objects.equals(this.message, errorReport.message)
        && Objects.equals(this.statusCode, errorReport.statusCode)
        && Objects.equals(this.causes, errorReport.causes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, statusCode, causes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorReport {\n");

    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
    sb.append("    causes: ").append(toIndentedString(causes)).append("\n");
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
