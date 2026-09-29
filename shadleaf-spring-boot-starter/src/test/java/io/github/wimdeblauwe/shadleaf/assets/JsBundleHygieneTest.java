package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks the compiled scripts: a page must end up with exactly one Alpine, and the csp variant must work under a
 * policy without {@code 'unsafe-eval'}.
 */
class JsBundleHygieneTest {

  // Dispatched by Alpine.start(); only a bundle that contains Alpine has it.
  private static final String ALPINE_SIGNATURE = "alpine:initialized";
  // The standard build evaluates expressions with Function(...), which 'unsafe-eval' is needed for.
  private static final String EVAL_SIGNATURE = "Function(";

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

  private static String bundle(AlpineVariant alpine) throws IOException {
    ViteManifest manifest = new ViteManifestParser(JsonMapper.builder().build())
        .parse(new ClassPathResource(ShadleafAssets.MANIFEST_LOCATION));
    String file = manifest.getEntry(alpine.jsEntry()).file();
    try (InputStream inputStream = new ClassPathResource("META-INF/resources/shadleaf/" + file).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
