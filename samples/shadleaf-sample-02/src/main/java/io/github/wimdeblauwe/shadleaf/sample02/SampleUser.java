package io.github.wimdeblauwe.shadleaf.sample02;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * A signed-in user with the name and email the user menu shows. A {@code UserDetails} only has a username, so the
 * sample extends Spring Security's {@link User}, as an application whose people live in its own database would, and
 * {@link SecurityConfiguration#currentUserResolver()} tells Shadleaf where to find the name.
 */
public class SampleUser extends User {

  private final String displayName;
  private final String email;

  public SampleUser(String username, String password, Collection<? extends GrantedAuthority> authorities,
                    String displayName, String email) {
    super(username, password, authorities);
    this.displayName = displayName;
    this.email = email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmail() {
    return email;
  }
}
