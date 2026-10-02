package io.github.wimdeblauwe.shadleaf.security;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The signed-in user, as the user menu shows them: what {@link CurrentUserResolver} makes of Spring Security's
 * {@code Authentication}, read in templates through {@code #slUser.current}.
 *
 * @param name     what to call the user: never {@code null}
 * @param username the name the user signs in with, such as GitHub's {@code login} or OpenID Connect's
 *                 {@code preferred_username}, for a second line when there is no email
 * @param email    the email address
 * @param picture  the URL of a picture, shown through {@code sl:avatar}; a broken one shows the initials
 * @param initials one or two letters for the avatar's fallback, or {@code null} when there is nothing to take them from
 */
public record ShadleafUser(String name, @Nullable String username, @Nullable String email, @Nullable String picture,
                           @Nullable String initials) {

  public ShadleafUser {
    Objects.requireNonNull(name, "name");
  }

  /** A user whose initials are taken from the name, or from the email when the name is the email. */
  public static ShadleafUser of(String name, @Nullable String username, @Nullable String email,
      @Nullable String picture) {
    return new ShadleafUser(name, username, email, picture, Initials.of(name, email));
  }
}
