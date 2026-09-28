package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.assets.AssetVariant;
import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The manifest test: for every skin and asset variant, the URL the Vite manifest resolves to is a real file in the
 * jar, served under /shadleaf/** to a visitor who has not signed in, with the one matcher the docs give.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AssetsBehindSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ViteManifestParser manifestParser;

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource({
      "default, STANDALONE",
      "default, EMBEDDED",
      "flat,    STANDALONE",
      "flat,    EMBEDDED"
  })
  void everyBundleIsServedAnonymously(String skin, AssetVariant variant) throws Exception {
    String cssUrl = new ShadleafAssets(skin, variant, null, manifestParser).getCssUrl();

    assertThat(cssUrl).startsWith("/shadleaf/");
    mockMvc.perform(get(cssUrl))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/css")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(".btn")));
  }

  @Test
  void theLoginPageLinksTheConfiguredBundle() throws Exception {
    String html = mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    String href = Jsoup.parse(html).head().selectFirst("link[href^=/shadleaf/]").attr("href");
    assertThat(href).isEqualTo(new ShadleafAssets("default", AssetVariant.STANDALONE, null, manifestParser)
        .getCssUrl());
    mockMvc.perform(get(href)).andExpect(status().isOk());
  }

  @Test
  void theViteManifestIsNotServed() throws Exception {
    mockMvc.perform(get("/shadleaf/.vite/manifest.json")).andExpect(status().isNotFound());
  }

  @Test
  void anythingElseStillNeedsSigningIn() throws Exception {
    mockMvc.perform(get("/orders")).andExpect(status().isFound());
  }
}
