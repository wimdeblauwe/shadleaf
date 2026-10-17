package io.github.wimdeblauwe.shadleaf.assets;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import java.io.IOException;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * Resolves the URLs of the library's assets: from the Vite dev server when {@code shadleaf.dev.vite-server-url} is
 * set, otherwise from the Vite manifest inside the jar; the files it names are served under {@code /shadleaf/**}.
 * The stylesheet is the build for the configured skin and asset variant, the script the build for the configured
 * {@link AlpineVariant}.
 */
public class ShadleafAssets {

  /** Outside {@code META-INF/resources}, so the manifest itself is never served. */
  public static final String MANIFEST_LOCATION = "shadleaf/vite-manifest.json";
  public static final String BASE_URL = "/shadleaf/";

  private boolean devMode;
  private String cssUrl;
  private String jsUrl;
  private @Nullable String viteClientUrl;

  public ShadleafAssets(Skin skin, AssetVariant variant, AlpineVariant alpine, @Nullable String viteServerUrl,
      ViteManifestParser viteManifestParser) {
    // Vite writes manifest keys relative to its `root` (src/main/resources/static)
    String cssEntry = variant.cssEntry(skin.id());
    String jsEntry = alpine.jsEntry();
    if (StringUtils.hasText(viteServerUrl)) {
      buildAssetsInDevMode(Objects.requireNonNull(viteServerUrl), cssEntry, jsEntry);
    } else {
      buildAssetsInBuildMode(viteManifestParser, skin, cssEntry, jsEntry);
    }
  }

  public boolean isDevMode() {
    return devMode;
  }

  public String getCssUrl() {
    return cssUrl;
  }

  /** The ES module to load: Alpine and the component registrations, or the registrations only. */
  public String getJsUrl() {
    return jsUrl;
  }

  public @Nullable String getViteClientUrl() {
    return viteClientUrl;
  }

  private void buildAssetsInDevMode(String viteServerUrl, String cssEntry, String jsEntry) {
    String base = stripTrailingSlash(viteServerUrl.trim());
    this.devMode = true;
    this.cssUrl = base + "/" + cssEntry;
    this.jsUrl = base + "/" + jsEntry;
    this.viteClientUrl = base + "/@vite/client";
  }

  private void buildAssetsInBuildMode(ViteManifestParser viteManifestParser, Skin skin, String cssEntry,
      String jsEntry) {
    this.devMode = false;
    try {
      ClassPathResource resource = new ClassPathResource(MANIFEST_LOCATION);
      if (!resource.exists()) {
        throw new IllegalStateException("Failed to read the Vite manifest at '" + MANIFEST_LOCATION + "'.");
      }
      ViteManifest manifest = viteManifestParser.parse(resource);
      if (!manifest.entries().containsKey(cssEntry)) {
        throw new IllegalStateException("The Vite manifest has no entry '%s' for skin '%s' (shadleaf.skin)."
            .formatted(cssEntry, skin.id()));
      }
      this.cssUrl = BASE_URL + manifest.getEntry(cssEntry).file();
      if (!manifest.entries().containsKey(jsEntry)) {
        throw new IllegalStateException("The Vite manifest has no entry '%s'.".formatted(jsEntry));
      }
      this.jsUrl = BASE_URL + manifest.getEntry(jsEntry).file();
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
