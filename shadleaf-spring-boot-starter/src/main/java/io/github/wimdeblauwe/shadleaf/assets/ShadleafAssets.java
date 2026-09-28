package io.github.wimdeblauwe.shadleaf.assets;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import java.io.IOException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * Resolves the URLs of the library's assets: from the Vite dev server when {@code shadleaf.dev.vite-server-url} is
 * set, otherwise from the Vite manifest inside the jar, served under {@code /shadleaf/**}.
 */
public class ShadleafAssets {

  public static final String MANIFEST_LOCATION = "META-INF/resources/shadleaf/.vite/manifest.json";
  public static final String BASE_URL = "/shadleaf/";
  // Vite writes manifest keys relative to its `root` (src/main/resources/static)
  private static final String CSS_MANIFEST_KEY = "css/shadleaf.css";

  private boolean devMode;
  private String cssUrl;
  private @Nullable String viteClientUrl;

  public ShadleafAssets(@Nullable String viteServerUrl,
      ViteManifestParser viteManifestParser) {
    if (StringUtils.hasText(viteServerUrl)) {
      buildAssetsInDevMode(Objects.requireNonNull(viteServerUrl));
    } else {
      buildAssetsInBuildMode(viteManifestParser);
    }
  }

  public boolean isDevMode() {
    return devMode;
  }

  public String getCssUrl() {
    return cssUrl;
  }

  public @Nullable String getViteClientUrl() {
    return viteClientUrl;
  }

  private void buildAssetsInDevMode(String viteServerUrl) {
    String base = stripTrailingSlash(viteServerUrl.trim());
    this.devMode = true;
    this.cssUrl = base + "/css/shadleaf.css";
    this.viteClientUrl = base + "/@vite/client";
  }

  private void buildAssetsInBuildMode(ViteManifestParser viteManifestParser) {
    this.devMode = false;
    try {
      ClassPathResource resource = new ClassPathResource(MANIFEST_LOCATION);
      if (!resource.exists()) {
        throw new IllegalStateException("Failed to read the Vite manifest at '" + MANIFEST_LOCATION + "'.");
      }
      ViteManifest manifest = viteManifestParser.parse(resource);
      this.cssUrl = BASE_URL + manifest.getEntry(CSS_MANIFEST_KEY).file();
      this.viteClientUrl = null;
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to read the Vite manifest at '" + MANIFEST_LOCATION + "'.", e);
    }
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
