package io.github.wimdeblauwe.shadleaf.sample02;

import io.github.wimdeblauwe.shadleaf.sample02.CspProperties.Mode;
import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

/**
 * Writes a strict Content-Security-Policy: everything from this origin only, and no {@code 'unsafe-inline'} for
 * scripts or styles. The one inline script, Shadleaf's theme script, is allowed by the request's nonce or by the hash
 * the library publishes.
 */
public class CspHeaderWriter implements HeaderWriter {

  static final String POLICY = "default-src 'self'; script-src 'self' %s; style-src 'self'; img-src 'self'; "
      + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

  private final Mode mode;
  private final ShadleafThemeScript themeScript;

  public CspHeaderWriter(Mode mode, ShadleafThemeScript themeScript) {
    this.mode = mode;
    this.themeScript = themeScript;
  }

  @Override
  public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
    String themeScriptSource = switch (mode) {
      case NONCE -> "'nonce-" + request.getAttribute(CspNonceFilter.ATTRIBUTE) + "'";
      case HASH -> themeScript.getCspHash();
    };
    response.setHeader("Content-Security-Policy", POLICY.formatted(themeScriptSource));
  }
}
