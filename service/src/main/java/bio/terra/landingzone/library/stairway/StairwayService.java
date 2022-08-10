package bio.terra.landingzone.library.stairway;

import bio.terra.common.stairway.StairwayComponent;

public class StairwayService {
  private StairwayComponent stairwayComponent;
  private StairwayComponent.StairwayOptionsBuilder stairwayOptionsBuilder;

  public StairwayService(
      StairwayComponent stairwayComponent,
      StairwayComponent.StairwayOptionsBuilder stairwayOptionsBuilder) {
    this.stairwayComponent = stairwayComponent;
    this.stairwayOptionsBuilder = stairwayOptionsBuilder;
  }

  public void initialize() {
    stairwayComponent.initialize(stairwayOptionsBuilder);
  }
}
