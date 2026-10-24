package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks the compiled scripts: a page must end up with exactly one Alpine, and the csp variant must work under a
 * policy without {@code 'unsafe-eval'}. So must the chart chunk, which every variant loads on demand.
 */
class JsBundleHygieneTest {

  // Dispatched by Alpine.start(); only a bundle that contains Alpine has it.
  private static final String ALPINE_SIGNATURE = "alpine:initialized";
  // The standard build evaluates expressions with Function(...), which 'unsafe-eval' is needed for.
  private static final String EVAL_SIGNATURE = "Function(";
  private static final String CHART_ENTRY = "js/chart/sl-chart.jsx";
  private static final String DECIMAL_GLOBAL_FALLBACK = "self:Function(`return this`)()";

  @Test
  void bundledContainsTheStandardAlpine() throws IOException {
    String js = bundle(AlpineVariant.BUNDLED);

    assertThat(js).contains(ALPINE_SIGNATURE).contains(EVAL_SIGNATURE);
  }

  @Test
  void cspContainsAnAlpineThatNeverEvaluatesStrings() throws IOException {
    String js = bundle(AlpineVariant.CSP);

    assertThat(js).contains(ALPINE_SIGNATURE).doesNotContain(EVAL_SIGNATURE).doesNotContain("eval(");
  }

  @Test
  void externalContainsNoAlpine() throws IOException {
    String js = bundle(AlpineVariant.EXTERNAL);

    assertThat(js).doesNotContain(ALPINE_SIGNATURE).doesNotContain(EVAL_SIGNATURE);
  }

  @Test
  void everyVariantLoadsTheChartChunkOnDemand() throws IOException {
    ViteManifest manifest = manifest();

    for (AlpineVariant alpine : AlpineVariant.values()) {
      List<String> dynamicImports = new ArrayList<>();
      for (String chunk : manifest.getEntry(alpine.jsEntry()).imports()) {
        dynamicImports.addAll(manifest.getEntry(chunk).dynamicImports());
      }
      assertThat(dynamicImports).as(alpine.name()).containsExactly(CHART_ENTRY);
    }
    assertThat(manifest.getEntry(CHART_ENTRY).isEntry()).isFalse();
  }

  @Test
  void chartChunkHoldsNoAlpineAndNeverEvaluatesStrings() throws IOException {
    String js = file(manifest().getEntry(CHART_ENTRY).file());

    // decimal.js-light's UMD wrapper falls back to Function('return this') for the global object where self is
    // undefined; never in a browser.
    assertThat(js.replace(DECIMAL_GLOBAL_FALLBACK, "")).doesNotContain(ALPINE_SIGNATURE)
        .doesNotContain(EVAL_SIGNATURE).doesNotContain("eval(");
  }

  @Test
  void chartChunkRunsRechartsOnPreact() throws IOException {
    String js = file(manifest().getEntry(CHART_ENTRY).file());

    // React's own runtime would bring its internals with it.
    assertThat(js).contains("recharts").doesNotContain("__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE");
  }

  private static String bundle(AlpineVariant alpine) throws IOException {
    return file(manifest().getEntry(alpine.jsEntry()).file());
  }

  private static ViteManifest manifest() throws IOException {
    return new ViteManifestParser(JsonMapper.builder().build())
        .parse(new ClassPathResource(ShadleafAssets.MANIFEST_LOCATION));
  }

  private static String file(String file) throws IOException {
    try (InputStream inputStream = new ClassPathResource("META-INF/resources/shadleaf/" + file).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
