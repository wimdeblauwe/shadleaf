package io.github.wimdeblauwe.shadleaf.security;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.ClassUtils;

/**
 * Everything that touches {@code spring-security-oauth2-core}, which an application with only form login does not
 * have: {@link DefaultCurrentUserResolver} only calls in here when {@link #PRESENT} is true.
 */
final class OAuth2Principals {

  static final boolean PRESENT = ClassUtils.isPresent(
      "org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal", OAuth2Principals.class.getClassLoader());

  private OAuth2Principals() {
  }

  /**
   * The attributes of an OAuth2 or OpenID Connect login (an {@code OidcUser}'s include its ID token's claims), or the
   * claims of a JWT; {@code null} for any other principal.
   */
  static @Nullable Map<String, Object> attributes(@Nullable Object principal) {
    if (principal instanceof OAuth2AuthenticatedPrincipal oauth2Principal) {
      return oauth2Principal.getAttributes();
    }
    if (principal instanceof ClaimAccessor claims) {
      return claims.getClaims();
    }
    return null;
  }
}
