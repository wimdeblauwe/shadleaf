package io.github.wimdeblauwe.shadleaf.security;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The {@link CurrentUserResolver} an application gets unless it declares its own. It reads, in this order:
 * <ul>
 *   <li>an OpenID Connect or OAuth2 login (and a JWT): the claims or attributes {@code name} (else {@code given_name}
 *   and {@code family_name}), {@code login} or {@code preferred_username}, {@code email}, and {@code picture} or
 *   {@code avatar_url}, as GitHub sends them. Without a name, the login, then the email, then
 *   {@link Authentication#getName()} (for GitHub the numeric id) stand in;</li>
 *   <li>a {@link UserDetails}: its username, as name and username;</li>
 *   <li>anything else: {@link Authentication#getName()}.</li>
 * </ul>
 */
public class DefaultCurrentUserResolver implements CurrentUserResolver {

  @Override
  public @Nullable ShadleafUser resolve(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (OAuth2Principals.PRESENT) {
      Map<String, Object> attributes = OAuth2Principals.attributes(principal);
      if (attributes != null) {
        return fromAttributes(attributes, authentication.getName());
      }
    }
    if (principal instanceof UserDetails userDetails) {
      return ShadleafUser.of(userDetails.getUsername(), userDetails.getUsername(), null, null);
    }
    String name = authentication.getName();
    return ShadleafUser.of(name, name, null, null);
  }

  private static ShadleafUser fromAttributes(Map<String, Object> attributes, String principalName) {
    String username = first(text(attributes, "login"), text(attributes, "preferred_username"));
    String email = text(attributes, "email");
    String name = first(text(attributes, "name"), fullName(attributes), username, email, principalName);
    String picture = first(text(attributes, "picture"), text(attributes, "avatar_url"));
    return ShadleafUser.of(name, username, email, picture);
  }

  private static @Nullable String fullName(Map<String, Object> attributes) {
    String given = text(attributes, "given_name");
    String family = text(attributes, "family_name");
    if (given == null || family == null) {
      return given != null ? given : family;
    }
    return given + " " + family;
  }

  private static @Nullable String text(Map<String, Object> attributes, String name) {
    return attributes.get(name) instanceof String value && !value.isBlank() ? value.strip() : null;
  }

  @SafeVarargs
  private static <T> T first(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return values[values.length - 1];
  }
}
