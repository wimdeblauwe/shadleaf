package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.security.ShadleafUser;
import io.github.wimdeblauwe.shadleaf.security.UserSource;
import org.jspecify.annotations.Nullable;

/**
 * A user source that always gives the same user, for {@code ComponentRenderTester.builder().userSource(...)}: a user
 * with exactly the fields a test or preview needs, without a Spring Security sign-in to derive them from.
 */
public record FixedUserSource(@Nullable ShadleafUser currentUser, String loginUrl, String logoutUrl)
    implements UserSource {

  public static FixedUserSource of(ShadleafUser user) {
    return new FixedUserSource(user, DEFAULT_LOGIN_URL, DEFAULT_LOGOUT_URL);
  }
}
