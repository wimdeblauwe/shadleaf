package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class ShadleafAssetsTest {

  private final ViteManifestParser parser = new ViteManifestParser(JsonMapper.builder().build());

  @Test
  void buildModeResolvesTheHashedCssFromTheManifest() {
    ShadleafAssets assets = new ShadleafAssets("vega", AssetVariant.STANDALONE, AlpineVariant.BUNDLED, null, parser);

    assertThat(assets.isDevMode()).isFalse();
    assertThat(assets.getViteClientUrl()).isNull();
    assertThat(assets.getCssUrl()).matches("/shadleaf/assets/shadleaf-vega-[\\w-]+\\.css");
  }

  @ParameterizedTest
  @CsvSource({
      "vega,    STANDALONE, shadleaf-vega-",
      "vega,    EMBEDDED,   shadleaf-vega.embedded-",
      "flat,    STANDALONE, shadleaf-flat-",
      "flat,    EMBEDDED,   shadleaf-flat.embedded-"
  })
  void buildModeCssUrlPointsAtARealClasspathResourceForEverySkinAndVariant(String skin, AssetVariant variant,
      String fileNamePrefix) {
    ShadleafAssets assets = new ShadleafAssets(skin, variant, AlpineVariant.BUNDLED, null, parser);

    assertThat(assets.getCssUrl()).startsWith("/shadleaf/assets/" + fileNamePrefix);
    // /shadleaf/** is served by Spring Boot from classpath:META-INF/resources/shadleaf/**
    String classpathLocation = "META-INF/resources" + assets.getCssUrl();
    assertThat(new ClassPathResource(classpathLocation).exists())
        .as("Expected %s on the classpath", classpathLocation)
        .isTrue();
  }

  @ParameterizedTest
  @CsvSource({
      "BUNDLED,  shadleaf.alpine-",
      "CSP,      shadleaf.alpine-csp-",
      "EXTERNAL, shadleaf-"
  })
  void buildModeJsUrlPointsAtARealClasspathResourceForEveryAlpineVariant(AlpineVariant alpine,
      String fileNamePrefix) {
    ShadleafAssets assets = new ShadleafAssets("vega", AssetVariant.STANDALONE, alpine, null, parser);

    assertThat(assets.getJsUrl()).matches("/shadleaf/assets/" + Pattern.quote(fileNamePrefix) + "[\\w-]+\\.js");
    String classpathLocation = "META-INF/resources" + assets.getJsUrl();
    assertThat(new ClassPathResource(classpathLocation).exists())
        .as("Expected %s on the classpath", classpathLocation)
        .isTrue();
  }

  @Test
  void unknownSkinFailsWithTheAvailableSkins() {
    assertThatThrownBy(() -> new ShadleafAssets("glossy", AssetVariant.STANDALONE, AlpineVariant.BUNDLED, null, parser))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown Shadleaf skin 'glossy' (shadleaf.skin). Available skins: [flat, vega]");
  }

  @Test
  void devModeUsesTheViteServer() {
    ShadleafAssets assets = new ShadleafAssets("vega", AssetVariant.STANDALONE, AlpineVariant.BUNDLED, "http://localhost:5174/", parser);

    assertThat(assets.isDevMode()).isTrue();
    assertThat(assets.getCssUrl()).isEqualTo("http://localhost:5174/css/entries/shadleaf-vega.css");
    assertThat(assets.getJsUrl()).isEqualTo("http://localhost:5174/js/entries/shadleaf.alpine.js");
    assertThat(assets.getViteClientUrl()).isEqualTo("http://localhost:5174/@vite/client");
  }

  @Test
  void devModeServesTheEntryForTheConfiguredSkinAndVariant() {
    ShadleafAssets assets = new ShadleafAssets("flat", AssetVariant.EMBEDDED, AlpineVariant.CSP, "http://localhost:5174",
        parser);

    assertThat(assets.getCssUrl()).isEqualTo("http://localhost:5174/css/entries/shadleaf-flat.embedded.css");
    assertThat(assets.getJsUrl()).isEqualTo("http://localhost:5174/js/entries/shadleaf.alpine-csp.js");
  }
}
