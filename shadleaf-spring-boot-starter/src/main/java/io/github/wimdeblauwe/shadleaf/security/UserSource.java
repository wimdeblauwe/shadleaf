package io.github.wimdeblauwe.shadleaf.security;

import org.jspecify.annotations.Nullable;

/**
 * Where {@code #slUser} gets the signed-in user and the sign-in and sign-out URLs from. With Spring Security on the
 * classpath it is {@link SpringSecurityUserSource}; without it, {@link #anonymous}: nobody is ever signed in, so
 * templates that show a user menu render a Sign in link instead of failing.
 * <p>
 * This interface names no Spring Security type, so the dialect can hold one in an application without Spring Security.
 * To change who the user is, declare a {@link CurrentUserResolver} bean, not one of these.
 */
public interface UserSource {

  String DEFAULT_LOGIN_URL = "/login";
  String DEFAULT_LOGOUT_URL = "/logout";

  /** The signed-in user, or {@code null} for an anonymous visitor. */
  @Nullable ShadleafUser currentUser();

  /** Where Sign in goes, within the application ({@code /login}). */
  String loginUrl();

  /** Where the sign-out form posts to, within the application ({@code /logout}). */
  String logoutUrl();

  /** Nobody is signed in, with the default URLs. */
  static UserSource anonymous() {
    return anonymous(DEFAULT_LOGIN_URL, DEFAULT_LOGOUT_URL);
  }

  /** Nobody is signed in. */
  static UserSource anonymous(String loginUrl, String logoutUrl) {
    return new UserSource() {
      @Override
      public @Nullable ShadleafUser currentUser() {
        return null;
      }

      @Override
      public String loginUrl() {
        return loginUrl;
      }

      @Override
      public String logoutUrl() {
        return logoutUrl;
      }
    };
  }
}
