package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks the compiled bundles, not the sources: these properties are what lets the library's stylesheet sit next to
 * an application's own Tailwind build.
 */
class CssBundleHygieneTest {

  // From preflight's `html, :host` rule; appears exactly once per copy of preflight.
  private static final String PREFLIGHT_SIGNATURE = "-webkit-text-size-adjust: 100%";

  static List<String> entries() throws IOException {
    return manifest().entries().keySet().stream()
        .filter(key -> key.startsWith("css/entries/"))
        .sorted()
        .toList();
  }

  @ParameterizedTest
  @MethodSource("entries")
  void noBundleEmitsUtilities(String entry) throws IOException {
    assertThat(layerBlock(bundle(entry), "utilities"))
        .as("utilities layer of %s", entry)
        .isNull();
  }

  @ParameterizedTest
  @MethodSource("entries")
  void standaloneBundlesContainPreflightOnceAndEmbeddedBundlesNever(String entry) throws IOException {
    int expected = isEmbedded(entry) ? 0 : 1;

    assertThat(occurrences(bundle(entry), PREFLIGHT_SIGNATURE))
        .as("copies of preflight in %s", entry)
        .isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("entries")
  void embeddedBundlesLeaveTheThemeLayerToTheApplication(String entry) throws IOException {
    String themeLayer = layerBlock(bundle(entry), "theme");

    if (isEmbedded(entry)) {
      // A --spacing or --font-sans here would override the application's own theme.
      assertThat(themeLayer).as("theme layer of %s", entry).isNull();
    } else {
      assertThat(themeLayer).as("theme layer of %s", entry).contains("--spacing");
    }
  }

  @ParameterizedTest
  @MethodSource("entries")
  void darkModeFollowsTheClassNotTheOperatingSystem(String entry) throws IOException {
    // A skin that uses dark: must get the class-based variant from tokens.css, never the media query.
    assertThat(bundle(entry)).doesNotContain("prefers-color-scheme");
  }

  @ParameterizedTest
  @MethodSource("entries")
  void tokensAreLayeredSoApplicationOverridesWin(String entry) throws IOException {
    assertThat(layerBlock(bundle(entry), "base"))
        .as("base layer of %s", entry)
        .contains("--primary:")
        .contains("--radius:");
  }

  private static boolean isEmbedded(String entry) {
    return entry.endsWith(".embedded.css");
  }

  /**
   * The body of the top-level {@code @layer name { ... }} block, or {@code null} when the bundle only declares the
   * layer (or does not mention it at all).
   */
  private static String layerBlock(String css, String name) {
    Matcher matcher = Pattern.compile("^@layer " + name + " \\{$", Pattern.MULTILINE).matcher(css);
    if (!matcher.find()) {
      return null;
    }
    int depth = 1;
    int i = matcher.end();
    while (depth > 0) {
      char c = css.charAt(i++);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
      }
    }
    return css.substring(matcher.end(), i - 1);
  }

  private static int occurrences(String haystack, String needle) {
    int count = 0;
    for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
      count++;
    }
    return count;
  }

  private static String bundle(String entry) throws IOException {
    String file = manifest().getEntry(entry).file();
    try (InputStream inputStream = new ClassPathResource("META-INF/resources/shadleaf/" + file).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static ViteManifest manifest() throws IOException {
    return new ViteManifestParser(JsonMapper.builder().build())
        .parse(new ClassPathResource(ShadleafAssets.MANIFEST_LOCATION));
  }
}
