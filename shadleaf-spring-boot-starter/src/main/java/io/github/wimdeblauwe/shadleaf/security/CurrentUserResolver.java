package io.github.wimdeblauwe.shadleaf.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Turns Spring Security's {@link Authentication} into the {@link ShadleafUser} that {@code #slUser} and the user menu
 * show. The default, {@link DefaultCurrentUserResolver}, reads OpenID Connect claims, OAuth2 attributes and
 * {@code UserDetails}; an application that knows its users better (a name from its own database, a picture it stores)
 * declares a bean of this type, which replaces the default.
 * <p>
 * Only called for a signed-in user: an anonymous visitor (no authentication, or one Spring Security's
 * {@code AuthenticationTrustResolver} calls anonymous) never gets here. Returning {@code null} treats the user as
 * anonymous too.
 */
@FunctionalInterface
public interface CurrentUserResolver {

  @Nullable ShadleafUser resolve(Authentication authentication);
}
