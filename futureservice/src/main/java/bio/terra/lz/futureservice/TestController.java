package bio.terra.lz.futureservice;

import bio.terra.landingzone.service.landingzone.azure.LandingZoneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  // no usage for now, but just make sure that lz library is linked
  LandingZoneService lzService;

  @GetMapping("/")
  public String index() {
    return "Test";
  }
}
