package io.github.wimdeblauwe.shadleaf.sample02;

import io.github.wimdeblauwe.shadleaf.sample02.CspProperties.Mode;
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
            .requestMatchers("/css/**", "/js/**").permitAll()
            .anyRequest().authenticated())
        .formLogin(login -> login.loginPage("/login").permitAll())
        .headers(headers -> headers.addHeaderWriter(new CspHeaderWriter(csp.mode(), themeScript)));
    if (csp.mode() == Mode.NONCE) {
      http.addFilterBefore(new CspNonceFilter(), HeaderWriterFilter.class);
    }
    return http.build();
  }
}
