package bio.terra.lz.futureservice.common.utils;

import bio.terra.lz.futureservice.app.controller.common.iam.AuthenticatedUserRequest;
import java.util.Optional;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MockMvcUtils {
  public static final String DEFAULT_USER_EMAIL = "fake@user.com";
  public static final String DEFAULT_USER_SUBJECT_ID = "subjectId123456";

  public static final AuthenticatedUserRequest USER_REQUEST =
      new AuthenticatedUserRequest(
          DEFAULT_USER_EMAIL, DEFAULT_USER_SUBJECT_ID, Optional.of("ThisIsNotARealBearerToken"));

  public static MockHttpServletRequestBuilder addAuth(
      MockHttpServletRequestBuilder request, AuthenticatedUserRequest userRequest) {
    return request.header("Authorization", "Bearer " + userRequest.getRequiredToken());
  }
}
