package io.github.wimdeblauwe.shadleaf.security;

import org.jspecify.annotations.Nullable;

/**
 * The expression object {@code #slUser}: the signed-in user ({@code #slUser.current}, {@code null} for an anonymous
 * visitor), whether there is one ({@code #slUser.signedIn}), and where to sign in and out ({@code #slUser.loginUrl},
 * {@code #slUser.logoutUrl}, paths within the application for {@code @{...}}).
 * <p>
 * An expression object rather than a model attribute, so it never clashes with an application's own {@code user} and
 * works where no controller adds anything: error pages, fragments. It is always there: without Spring Security nobody
 * is signed in. The user is resolved once per template, on first use.
 */
public final class CurrentUser {

  private final UserSource source;
  private boolean resolved;
  private @Nullable ShadleafUser user;

  public CurrentUser(UserSource source) {
    this.source = source;
  }

  /** The signed-in user, or {@code null}. */
  public @Nullable ShadleafUser getCurrent() {
    if (!resolved) {
      user = source.currentUser();
      resolved = true;
    }
    return user;
  }

  public boolean isSignedIn() {
    return getCurrent() != null;
  }

  public String getLoginUrl() {
    return source.loginUrl();
  }

  public String getLogoutUrl() {
    return source.logoutUrl();
  }
}
