package io.github.wimdeblauwe.shadleaf;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for Shadleaf, bound under the {@code shadleaf} prefix.
 *
 * @param dev settings for working on the library itself with live reload
 */
@ConfigurationProperties(prefix = "shadleaf")
public record ShadleafProperties(@DefaultValue DevProperties dev) {

  /**
   * @param viteServerUrl URL of the library's Vite dev server (e.g. {@code http://localhost:5174}). When set, the CSS is
   *                      loaded from there instead of from the jar.
   * @param templatesPath filesystem path to the library's {@code src/main/resources/templates/} directory. When set,
   *                      the library templates are read uncached from disk so edits show up without a rebuild.
   */
  public record DevProperties(@Nullable String viteServerUrl, @Nullable String templatesPath) {

  }
}
