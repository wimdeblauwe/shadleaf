package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class ShadleafAssetsTest {

  private final ViteManifestParser parser = new ViteManifestParser(JsonMapper.builder().build());

  @Test
  void buildModeResolvesTheHashedCssFromTheManifest() {
    ShadleafAssets assets = new ShadleafAssets(null, parser);

    assertThat(assets.isDevMode()).isFalse();
    assertThat(assets.getViteClientUrl()).isNull();
    assertThat(assets.getCssUrl()).startsWith("/shadleaf/assets/").endsWith(".css");
  }

  @Test
  void buildModeCssUrlPointsAtARealClasspathResource() {
    ShadleafAssets assets = new ShadleafAssets(null, parser);

    // /shadleaf/** is served by Spring Boot from classpath:META-INF/resources/shadleaf/**
    String classpathLocation = "META-INF/resources" + assets.getCssUrl();
    assertThat(new ClassPathResource(classpathLocation).exists())
        .as("Expected %s on the classpath", classpathLocation)
        .isTrue();
  }

  @Test
  void devModeUsesTheViteServer() {
    ShadleafAssets assets = new ShadleafAssets("http://localhost:5174/", parser);

    assertThat(assets.isDevMode()).isTrue();
    assertThat(assets.getCssUrl()).isEqualTo("http://localhost:5174/css/shadleaf.css");
    assertThat(assets.getViteClientUrl()).isEqualTo("http://localhost:5174/@vite/client");
  }
}
