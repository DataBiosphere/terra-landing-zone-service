package bio.terra.landingzone.service.iam;

public class SamConstants {

  public static class SamResourceType {
    public static final String SPEND_PROFILE = "spend-profile";
    public static final String LANDING_ZONE = "landing-zone";

    private SamResourceType() {}
  }

  public static class SamRole {
    public static final String OWNER = "owner";
    public static final String USER = "user";

    private SamRole() {}
  }

  public static class SamSpendProfileAction {
    public static final String LINK = "link";

    private SamSpendProfileAction() {}
  }

  public static class SamLandingZoneAction {
    public static final String LIST_RESOURCES = "list_resources";
    public static final String DELETE = "delete";

    private SamLandingZoneAction() {}
  }
}
