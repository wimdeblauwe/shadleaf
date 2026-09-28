package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class ShadleafAssetsTest {

  private final ViteManifestParser parser = new ViteManifestParser(JsonMapper.builder().build());

  @Test
  void buildModeResolvesTheHashedCssFromTheManifest() {
    ShadleafAssets assets = new ShadleafAssets("default", AssetVariant.STANDALONE, null, parser);

    assertThat(assets.isDevMode()).isFalse();
    assertThat(assets.getViteClientUrl()).isNull();
    assertThat(assets.getCssUrl()).matches("/shadleaf/assets/shadleaf-default-[\\w-]+\\.css");
  }

  @ParameterizedTest
  @CsvSource({
      "default, STANDALONE, shadleaf-default-",
      "default, EMBEDDED,   shadleaf-default.embedded-",
      "flat,    STANDALONE, shadleaf-flat-",
      "flat,    EMBEDDED,   shadleaf-flat.embedded-"
  })
  void buildModeCssUrlPointsAtARealClasspathResourceForEverySkinAndVariant(String skin, AssetVariant variant,
      String fileNamePrefix) {
    ShadleafAssets assets = new ShadleafAssets(skin, variant, null, parser);

    assertThat(assets.getCssUrl()).startsWith("/shadleaf/assets/" + fileNamePrefix);
    // /shadleaf/** is served by Spring Boot from classpath:META-INF/resources/shadleaf/**
    String classpathLocation = "META-INF/resources" + assets.getCssUrl();
    assertThat(new ClassPathResource(classpathLocation).exists())
        .as("Expected %s on the classpath", classpathLocation)
        .isTrue();
  }

  @Test
  void unknownSkinFailsWithTheAvailableSkins() {
    assertThatThrownBy(() -> new ShadleafAssets("glossy", AssetVariant.STANDALONE, null, parser))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown Shadleaf skin 'glossy' (shadleaf.skin). Available skins: [default, flat]");
  }

  @Test
  void devModeUsesTheViteServer() {
    ShadleafAssets assets = new ShadleafAssets("default", AssetVariant.STANDALONE, "http://localhost:5174/", parser);

    assertThat(assets.isDevMode()).isTrue();
    assertThat(assets.getCssUrl()).isEqualTo("http://localhost:5174/css/entries/shadleaf-default.css");
    assertThat(assets.getViteClientUrl()).isEqualTo("http://localhost:5174/@vite/client");
  }

  @Test
  void devModeServesTheEntryForTheConfiguredSkinAndVariant() {
    ShadleafAssets assets = new ShadleafAssets("flat", AssetVariant.EMBEDDED, "http://localhost:5174", parser);

    assertThat(assets.getCssUrl()).isEqualTo("http://localhost:5174/css/entries/shadleaf-flat.embedded.css");
  }
}
