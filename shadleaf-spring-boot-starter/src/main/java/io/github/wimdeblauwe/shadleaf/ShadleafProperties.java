package io.github.wimdeblauwe.shadleaf;

import io.github.wimdeblauwe.shadleaf.assets.AlpineVariant;
import io.github.wimdeblauwe.shadleaf.assets.AssetVariant;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for Shadleaf, bound under the {@code shadleaf} prefix.
 *
 * @param skin   the visual identity of the components, named after the shadcn/ui style it follows:
 *               {@code vega} (the default, shadcn's classic look) or {@code lyra} (boxy and sharp). Same markup and
 *               tokens, different geometry, type size and shadow.
 * @param assets which builds of the stylesheet and the script to load
 * @param csp    Content-Security-Policy support for the inline theme script
 * @param dev    settings for working on the library itself with live reload
 */
@ConfigurationProperties(prefix = "shadleaf")
public record ShadleafProperties(@DefaultValue("vega") String skin,
                                 @DefaultValue AssetsProperties assets,
                                 @DefaultValue CspProperties csp,
                                 @DefaultValue DevProperties dev,
                                 @DefaultValue SecurityProperties security) {

  /**
   * @param variant {@code standalone} (the default) includes Tailwind's preflight reset; {@code embedded} leaves it
   *                out, for applications that compile Tailwind themselves
   * @param alpine  where the page's Alpine.js comes from: {@code bundled} (the default) ships Alpine's standard build,
   *                {@code csp} its CSP build for a policy without {@code 'unsafe-eval'}, and {@code external} only
   *                Shadleaf's component registrations, for an application that loads Alpine itself
   */
  public record AssetsProperties(@DefaultValue("standalone") AssetVariant variant,
                                 @DefaultValue("bundled") AlpineVariant alpine) {

  }

  /**
   * @param nonceAttribute name of the request attribute holding the CSP nonce. When the request carries it, the theme
   *                       script is rendered with a {@code nonce} attribute.
   */
  public record CspProperties(@DefaultValue("cspNonce") String nonceAttribute) {

  }

  /**
   * @param viteServerUrl URL of the library's Vite dev server (e.g. {@code http://localhost:5174}). When set, the CSS and
   *                      JS are loaded from there instead of from the jar.
   * @param templatesPath filesystem path to the library's {@code src/main/resources/templates/} directory. When set,
   *                      the library templates are read uncached from disk so edits show up without a rebuild.
   * @param webTypesFile  file to write the web-types for IntelliJ IDEA and WebStorm to, e.g.
   *                      {@code shadleaf.web-types.json}; a relative path resolves against the working directory. It
   *                      describes this application's components, overrides included, and is referenced from the
   *                      {@code web-types} property of a {@code package.json} next to it. Meant for development only.
   */
  public record DevProperties(@Nullable String viteServerUrl, @Nullable String templatesPath,
                              @Nullable String webTypesFile) {

  }

  /**
   * Where the user menu sends a visitor to sign in and out. Only used with Spring Security on the classpath; without it
   * nobody is ever signed in, and the menu shows a Sign in link to {@code login-url}.
   *
   * @param loginUrl  where Sign in goes, a path within the application. Defaults to Spring Security's authorization
   *                  endpoint ({@code /oauth2/authorization/<id>}) when exactly one OAuth2 login client is registered,
   *                  else {@code /login}
   * @param logoutUrl where the sign-out form posts to, a path within the application
   */
  public record SecurityProperties(@Nullable String loginUrl, @DefaultValue("/logout") String logoutUrl) {

  }
}
