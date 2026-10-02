package io.github.wimdeblauwe.shadleaf.sample02;

import io.github.wimdeblauwe.shadleaf.sample02.CspProperties.Mode;
import io.github.wimdeblauwe.shadleaf.security.CurrentUserResolver;
import io.github.wimdeblauwe.shadleaf.security.DefaultCurrentUserResolver;
import io.github.wimdeblauwe.shadleaf.security.ShadleafUser;
import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, CspProperties csp, ShadleafThemeScript themeScript) {
    http
        .authorizeHttpRequests(requests -> requests
            // Everything Shadleaf serves lives under /shadleaf/**: one matcher for the library's assets.
            .requestMatchers("/shadleaf/**").permitAll()
            .requestMatchers("/css/**").permitAll()
            // This rule is the security: the layout only hides the Admin link from users who would get a 403 here.
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .formLogin(login -> login.loginPage("/login").permitAll())
        // formLogin's permitAll matches /login exactly, query included: without this, /login?logout needs a sign-in,
        // gets saved as the request to return to, and the next sign-in lands on the "signed out" page again.
        .logout(logout -> logout.permitAll())
        .headers(headers -> headers.addHeaderWriter(new CspHeaderWriter(csp.mode(), themeScript)));
    if (csp.mode() == Mode.NONCE) {
      http.addFilterBefore(new CspNonceFilter(), HeaderWriterFilter.class);
    }
    return http.build();
  }

  @Bean
  SampleUsers userDetailsService() {
    return new SampleUsers();
  }

  /**
   * Shadleaf's default resolver only knows a {@code UserDetails}' username; this one shows a {@link SampleUser}'s name
   * and email in the user menu, and leaves anything else (a test's plain {@code User}) to the default.
   */
  @Bean
  CurrentUserResolver currentUserResolver() {
    CurrentUserResolver fallback = new DefaultCurrentUserResolver();
    return authentication -> authentication.getPrincipal() instanceof SampleUser user
        ? ShadleafUser.of(user.getDisplayName(), user.getUsername(), user.getEmail(), null)
        : fallback.resolve(authentication);
  }
}
