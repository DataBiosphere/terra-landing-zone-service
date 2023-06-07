package bio.terra.landingzone.stairway.flight.create.resource.step;

import java.util.List;

public class AlertRuleTemplates {
  private AlertRuleTemplates() {}

  public static List<String> getSentinelScheduledAlertRuleTemplateIds() {
    return List.of(
        "0b9ae89d-8cad-461c-808f-0494f70ad5c4",
        "8ee967a2-a645-4832-85f4-72b635bcb3a6",
        "1ce5e766-26ab-4616-b7c8-3b33ae321e80",
        "532f62c1-fba6-4baa-bbb6-4a32a4ef32fa",
        "e1ce0eab-10d1-4aae-863f-9a383345ba88",
        "90d3f6ec-80fb-48e0-9937-2c70c9df9bad",
        "80733eb7-35b2-45b6-b2b8-3c51df258206",
        "b31037ea-6f68-4fbd-bab2-d0d0f44c2fcf",
        "e7ec9fa6-e7f7-41ed-a34b-b956837a3ee6");
  }

  public static List<String> getSentinelMlRuleTemplateIds() {
    return List.of("fa118b98-de46-4e94-87f9-8e6d5060b60b");
  }

  public static List<String> getSentinelNrtRuleTemplateIds() {
    return List.of("dd03057e-4347-4853-bf1e-2b2d21eb4e59");
  }
}
