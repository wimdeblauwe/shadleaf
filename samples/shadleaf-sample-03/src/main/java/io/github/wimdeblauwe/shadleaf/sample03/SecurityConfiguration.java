package io.github.wimdeblauwe.shadleaf.sample03;

import io.github.wimdeblauwe.htmx.spring.boot.security.HxRedirectToPageAccessDeniedHandler;
import io.github.wimdeblauwe.htmx.spring.boot.security.HxRedirectToPageAuthenticationEntryPoint;
import io.github.wimdeblauwe.shadleaf.security.UserSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clients, UserSource users) {
    // Signing out ends the session at Keycloak too (OpenID Connect RP-initiated logout): without it, the next Sign in
    // would go straight through, as Keycloak still knows the user. Keycloak sends the browser back to the home page,
    // which asks to sign in again. A GitHub user only leaves this application (GitHub has no such endpoint).
    OidcClientInitiatedLogoutSuccessHandler signOutAtKeycloak = new OidcClientInitiatedLogoutSuccessHandler(clients);
    signOutAtKeycloak.setPostLogoutRedirectUri("{baseUrl}/");
    return http
        .authorizeHttpRequests(requests -> requests
            .requestMatchers("/shadleaf/**").permitAll()
            .requestMatchers("/css/**", "/webjars/**").permitAll()
            // The path, whatever the query: oauth2Login's permitAll() matches /login and /login?error exactly.
            .requestMatchers("/login").permitAll()
            .anyRequest().authenticated())
        // The page to choose a provider on. Spring Security generates one, but not once the exception handling below
        // sets an entry point of its own: then /login is the application's (LoginController).
        .oauth2Login(login -> login.loginPage("/login"))
        .logout(logout -> logout.logoutSuccessHandler(signOutAtKeycloak))
        // An htmx request after the session expired is sent to the page itself (HX-Redirect), a normal navigation
        // that the entry point below answers. users.loginUrl() is where Sign in links to: with one client (Keycloak)
        // straight to it, with the github profile's two the page above.
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new HxRedirectToPageAuthenticationEntryPoint(
                new LoginUrlAuthenticationEntryPoint(users.loginUrl())))
            .accessDeniedHandler(new HxRedirectToPageAccessDeniedHandler()))
        .build();
  }
}
