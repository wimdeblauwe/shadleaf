package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.security.DefaultCurrentUserResolver;
import io.github.wimdeblauwe.shadleaf.security.SpringSecurityUserSource;
import io.github.wimdeblauwe.shadleaf.security.UserSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.util.ClassUtils;

/**
 * The tester's only contact with Spring Security, so it also runs in the tests without it on the classpath: the
 * {@link UserSource} an application with the default resolver gets, and the security context a request post-processor
 * such as {@code oidcLogin()} saved for the request, loaded as Spring Security's {@code SecurityContextHolderFilter}
 * loads it.
 */
final class TesterSecurity {

  static final boolean PRESENT = ClassUtils.isPresent(
      "org.springframework.security.test.context.TestSecurityContextHolder", TesterSecurity.class.getClassLoader());

  private TesterSecurity() {
  }

  static UserSource defaultUserSource() {
    return PRESENT
        ? new SpringSecurityUserSource(new DefaultCurrentUserResolver(), UserSource.DEFAULT_LOGIN_URL,
            UserSource.DEFAULT_LOGOUT_URL)
        : UserSource.anonymous();
  }

  /** Loads the request's security context into {@link SecurityContextHolder}, as the filter chain would. */
  static void loadContext(HttpServletRequest request) {
    if (PRESENT) {
      SecurityContextHolder.setContext(WebTestUtils.getSecurityContextRepository(request)
          .loadDeferredContext(request).get());
    }
  }

  static void clearContext() {
    if (PRESENT) {
      TestSecurityContextHolder.clearContext();
      SecurityContextHolder.clearContext();
    }
  }
}
