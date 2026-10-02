package io.github.wimdeblauwe.shadleaf.sample02;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/**
 * The sample's two users, both with the password {@code password}: Ada is an admin, Grace is not. Spring Security's
 * {@code InMemoryUserDetailsManager} would hand out plain {@code User}s and lose the name, so the sample keeps its own
 * list.
 */
public class SampleUsers implements UserDetailsService {

  public static final List<Account> ACCOUNTS = List.of(
      new Account("ada", "Ada Lovelace", "ada@example.com", List.of("ADMIN", "USER")),
      new Account("grace", "Grace Hopper", "grace@example.com", List.of("USER")));

  private final Map<String, Account> accounts;
  private final String password = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("password");

  public SampleUsers() {
    accounts = ACCOUNTS.stream().collect(Collectors.toMap(Account::username, Function.identity()));
  }

  /** A new user every time: Spring Security erases the password of the user it signed in. */
  @Override
  public SampleUser loadUserByUsername(String username) {
    Account account = accounts.get(username);
    if (account == null) {
      throw new UsernameNotFoundException(username);
    }
    return new SampleUser(account.username(), password,
        AuthorityUtils.createAuthorityList(account.roles().stream().map(role -> "ROLE_" + role).toList()),
        account.name(), account.email());
  }

  public record Account(String username, String name, String email, List<String> roles) {

  }
}
