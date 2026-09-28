package io.github.wimdeblauwe.shadleaf.sample02;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How the Content-Security-Policy allows Shadleaf's inline theme script: {@code nonce} (the default) gives every
 * request its own nonce; {@code hash} sends a fixed policy carrying {@code ShadleafThemeScript#getCspHash()}.
 */
@ConfigurationProperties("sample.csp")
public record CspProperties(@DefaultValue("nonce") Mode mode) {

  public enum Mode {
    NONCE,
    HASH
  }
}
