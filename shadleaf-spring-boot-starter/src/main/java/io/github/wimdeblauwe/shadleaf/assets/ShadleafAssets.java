package io.github.wimdeblauwe.shadleaf.assets;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import java.io.IOException;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * Resolves the URLs of the library's assets: from the Vite dev server when {@code shadleaf.dev.vite-server-url} is
 * set, otherwise from the Vite manifest inside the jar, served under {@code /shadleaf/**}. The stylesheet is the
 * build for the configured skin and asset variant.
 */
public class ShadleafAssets {

  public static final String MANIFEST_LOCATION = "META-INF/resources/shadleaf/.vite/manifest.json";
  public static final String BASE_URL = "/shadleaf/";
  private static final Pattern SKIN_ENTRY = Pattern.compile("css/entries/shadleaf-([^.]+)\\.css");

  private boolean devMode;
  private String cssUrl;
  private @Nullable String viteClientUrl;

  public ShadleafAssets(String skin, AssetVariant variant, @Nullable String viteServerUrl,
      ViteManifestParser viteManifestParser) {
    // Vite writes manifest keys relative to its `root` (src/main/resources/static)
    String cssEntry = variant.cssEntry(skin);
    if (StringUtils.hasText(viteServerUrl)) {
      buildAssetsInDevMode(Objects.requireNonNull(viteServerUrl), cssEntry);
    } else {
      buildAssetsInBuildMode(viteManifestParser, skin, cssEntry);
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

  private void buildAssetsInDevMode(String viteServerUrl, String cssEntry) {
    String base = stripTrailingSlash(viteServerUrl.trim());
    this.devMode = true;
    this.cssUrl = base + "/" + cssEntry;
    this.viteClientUrl = base + "/@vite/client";
  }

  private void buildAssetsInBuildMode(ViteManifestParser viteManifestParser, String skin, String cssEntry) {
    this.devMode = false;
    try {
      ClassPathResource resource = new ClassPathResource(MANIFEST_LOCATION);
      if (!resource.exists()) {
        throw new IllegalStateException("Failed to read the Vite manifest at '" + MANIFEST_LOCATION + "'.");
      }
      ViteManifest manifest = viteManifestParser.parse(resource);
      if (!manifest.entries().containsKey(cssEntry)) {
        throw new IllegalStateException("Unknown Shadleaf skin '%s' (shadleaf.skin). Available skins: %s"
            .formatted(skin, availableSkins(manifest)));
      }
      this.cssUrl = BASE_URL + manifest.getEntry(cssEntry).file();
      this.viteClientUrl = null;
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to read the Vite manifest at '" + MANIFEST_LOCATION + "'.", e);
    }
  }

  private static TreeSet<String> availableSkins(ViteManifest manifest) {
    TreeSet<String> skins = new TreeSet<>();
    for (String key : manifest.entries().keySet()) {
      Matcher matcher = SKIN_ENTRY.matcher(key);
      if (matcher.matches()) {
        skins.add(matcher.group(1));
      }
    }
    return skins;
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
