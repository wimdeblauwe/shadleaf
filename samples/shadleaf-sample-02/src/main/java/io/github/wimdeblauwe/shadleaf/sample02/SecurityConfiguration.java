package io.github.wimdeblauwe.shadleaf.sample02;

import io.github.wimdeblauwe.htmx.spring.boot.security.HxRedirectToPageAccessDeniedHandler;
import io.github.wimdeblauwe.htmx.spring.boot.security.HxRedirectToPageAuthenticationEntryPoint;
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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, CspProperties csp, ShadleafThemeScript themeScript) {
    http
        .authorizeHttpRequests(requests -> requests
            // Everything Shadleaf serves lives under /shadleaf/**: one matcher for the library's assets.
            .requestMatchers("/shadleaf/**").permitAll()
            .requestMatchers("/css/**", "/webjars/**").permitAll()
            // This rule is the security: the layout only hides the Admin link from users who would get a 403 here.
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .formLogin(login -> login.loginPage("/login").permitAll())
        // formLogin's permitAll matches /login exactly, query included: without this, /login?logout needs a sign-in,
        // gets saved as the request to return to, and the next sign-in lands on the "signed out" page again.
        .logout(logout -> logout.permitAll())
        // An htmx request after the session expired (a boosted link or form, the Data page's lazy tab) would get the
        // sign-in page swapped into its target, or, for a POST whose CSRF token went with the session, a 403 that
        // htmx does not show. These send the browser to the page instead (HX-Redirect): a normal navigation, which
        // the login form above answers and saves as the page to return to. Every other request goes to the login
        // form's entry point, as without them.
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new HxRedirectToPageAuthenticationEntryPoint(
                new LoginUrlAuthenticationEntryPoint("/login")))
            .accessDeniedHandler(new HxRedirectToPageAccessDeniedHandler()))
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
