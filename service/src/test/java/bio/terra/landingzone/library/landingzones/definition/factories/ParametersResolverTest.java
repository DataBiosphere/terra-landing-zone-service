package bio.terra.landingzone.library.landingzones.definition.factories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParametersResolverTest {

  private Map<String, String> defaultParams;

  private Map<String, String> parameters;

  @BeforeEach
  void setUp() {
    defaultParams = new HashMap<>();
    parameters = new HashMap<>();
  }

  @Test
  void getValue_withoutValueSetReturnsDefault() {
    defaultParams.put("FOO", "BAR");
    var resolver = new ParametersResolver(parameters, defaultParams);

    assertThat(resolver.getValue("FOO"), equalTo("BAR"));
  }

  @Test
  void getValue_paramsAreNullReturnsDefault() {
    defaultParams.put("FOO", "BAR");
    var resolver = new ParametersResolver(null, defaultParams);

    assertThat(resolver.getValue("FOO"), equalTo("BAR"));
  }

  @Test
  void getValue_paramsExistsWithDefaultReturnsValue() {
    defaultParams.put("FOO", "BAR");
    parameters.put("FOO", "BAR2");
    var resolver = new ParametersResolver(parameters, defaultParams);

    assertThat(resolver.getValue("FOO"), equalTo("BAR2"));
  }

  @Test
  void getValue_paramsExistsWithoutDefaultReturnsValue() {
    parameters.put("FOO", "BAR");
    var resolver = new ParametersResolver(parameters, defaultParams);

    assertThat(resolver.getValue("FOO"), equalTo("BAR"));
  }

  @Test
  void getValue_paramsAreNullWithoutDefaultReturnsEmpty() {
    var resolver = new ParametersResolver(null, defaultParams);

    assertThat(resolver.getValue("FOO"), equalTo(""));
  }
}
