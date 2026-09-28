package io.github.wimdeblauwe.shadleaf.theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.core.io.ClassPathResource;

/**
 * The pre-paint dark mode script, rendered inline by the {@code sl/layout :: theme-script} fragment.
 * <p>
 * It reads {@code localStorage['shadleaf-theme']} ({@code light} or {@code dark}), falls back to
 * {@code prefers-color-scheme}, and sets the {@code dark} class on {@code <html>} before the first paint.
 * <p>
 * Under a Content-Security-Policy, the script is allowed either by a nonce (read from the request attribute named by
 * {@code shadleaf.csp.nonce-attribute}) or by its hash: {@link #getCspHash()} is computed from the exact content the
 * fragment renders, so a policy built from it cannot go stale on upgrade.
 */
public class ShadleafThemeScript {

  public static final String LOCATION = "shadleaf/theme-script.js";

  private final String content;
  private final String cspHash;
  private final String nonceAttribute;

  public ShadleafThemeScript(String nonceAttribute) {
    this.content = load().strip();
    this.cspHash = "'sha256-" + sha256Base64(content) + "'";
    this.nonceAttribute = nonceAttribute;
  }

  /**
   * The script, exactly as it is rendered between {@code <script>} and {@code </script>}.
   */
  public String getContent() {
    return content;
  }

  /**
   * The hash source for a {@code script-src} directive, quotes included, e.g. {@code 'sha256-…'}.
   */
  public String getCspHash() {
    return cspHash;
  }

  /**
   * The name of the request attribute that holds the CSP nonce.
   */
  public String getNonceAttribute() {
    return nonceAttribute;
  }

  private static String load() {
    try (InputStream inputStream = new ClassPathResource(LOCATION).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read the Shadleaf theme script at '" + LOCATION + "'.", e);
    }
  }

  private static String sha256Base64(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }
}
