package io.github.wimdeblauwe.shadleaf.sample02;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a fresh CSP nonce in the {@value #ATTRIBUTE} request attribute. Thymeleaf sees request
 * attributes as context variables, which is where {@code sl/layout :: theme-script} looks for it (the attribute name
 * is {@code shadleaf.csp.nonce-attribute}, default {@code cspNonce}). {@link CspHeaderWriter} puts the same value in
 * the header.
 * <p>
 * Spring Security 7 has no nonce support of its own for servlet applications, so this runs in its filter chain,
 * before the {@code HeaderWriterFilter}.
 */
public class CspNonceFilter extends OncePerRequestFilter {

  public static final String ATTRIBUTE = "cspNonce";

  private final SecureRandom random = new SecureRandom();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    byte[] nonce = new byte[16];
    random.nextBytes(nonce);
    request.setAttribute(ATTRIBUTE, Base64.getEncoder().encodeToString(nonce));
    chain.doFilter(request, response);
  }
}
