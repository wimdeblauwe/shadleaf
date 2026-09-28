package io.github.wimdeblauwe.shadleaf;

import io.github.wimdeblauwe.shadleaf.assets.AssetVariant;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for Shadleaf, bound under the {@code shadleaf} prefix.
 *
 * @param skin   the visual identity of the components: {@code default} or {@code flat}. Same markup and tokens,
 *               different geometry, weight and shadow.
 * @param assets which build of the stylesheet to load
 * @param csp    Content-Security-Policy support for the inline theme script
 * @param dev    settings for working on the library itself with live reload
 */
@ConfigurationProperties(prefix = "shadleaf")
public record ShadleafProperties(@DefaultValue("default") String skin,
                                 @DefaultValue AssetsProperties assets,
                                 @DefaultValue CspProperties csp,
                                 @DefaultValue DevProperties dev) {

  /**
   * @param variant {@code standalone} (the default) includes Tailwind's preflight reset; {@code embedded} leaves it
   *                out, for applications that compile Tailwind themselves
   */
  public record AssetsProperties(@DefaultValue("standalone") AssetVariant variant) {

  }

  /**
   * @param nonceAttribute name of the request attribute holding the CSP nonce. When the request carries it, the theme
   *                       script is rendered with a {@code nonce} attribute.
   */
  public record CspProperties(@DefaultValue("cspNonce") String nonceAttribute) {

  }

  /**
   * @param viteServerUrl URL of the library's Vite dev server (e.g. {@code http://localhost:5174}). When set, the CSS is
   *                      loaded from there instead of from the jar.
   * @param templatesPath filesystem path to the library's {@code src/main/resources/templates/} directory. When set,
   *                      the library templates are read uncached from disk so edits show up without a rebuild.
   */
  public record DevProperties(@Nullable String viteServerUrl, @Nullable String templatesPath) {

  }
}
