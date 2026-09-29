package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.assets.AlpineVariant;
import io.github.wimdeblauwe.shadleaf.assets.AssetVariant;
import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The manifest test: for every skin and asset variant, and every Alpine variant, the URL the Vite manifest resolves to
 * is a real file in the jar, served under /shadleaf/** to a visitor who has not signed in, with the one matcher the
 * docs give. So are the chunks a script imports.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AssetsBehindSecurityTest {

  // How Vite's output imports a shared chunk: from"./register-BrzqsZGo.js"
  private static final Pattern RELATIVE_IMPORT = Pattern.compile("from\\s*\"\\./([^\"]+\\.js)\"");

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
    String cssUrl = new ShadleafAssets(skin, variant, AlpineVariant.BUNDLED, null, manifestParser).getCssUrl();

    assertThat(cssUrl).startsWith("/shadleaf/");
    mockMvc.perform(get(cssUrl))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/css")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(".btn")));
  }

  @ParameterizedTest
  @EnumSource(AlpineVariant.class)
  void everyScriptAndTheChunksItImportsAreServedAnonymously(AlpineVariant alpine) throws Exception {
    String jsUrl = new ShadleafAssets("default", AssetVariant.STANDALONE, alpine, null, manifestParser).getJsUrl();

    assertThat(jsUrl).startsWith("/shadleaf/");
    String script = mockMvc.perform(get(jsUrl))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/javascript")))
        .andReturn().getResponse().getContentAsString();
    Matcher imports = RELATIVE_IMPORT.matcher(script);
    while (imports.find()) {
      String chunkUrl = jsUrl.substring(0, jsUrl.lastIndexOf('/') + 1) + imports.group(1);
      mockMvc.perform(get(chunkUrl)).andExpect(status().isOk());
    }
  }

  @Test
  void theLoginPageLinksTheConfiguredBundle() throws Exception {
    String html = mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    String href = Jsoup.parse(html).head().selectFirst("link[href^=/shadleaf/]").attr("href");
    assertThat(href).isEqualTo(new ShadleafAssets("default", AssetVariant.STANDALONE, AlpineVariant.CSP, null,
        manifestParser).getCssUrl());
    mockMvc.perform(get(href)).andExpect(status().isOk());
  }

  @Test
  void theLoginPageLoadsAlpinesCspBuild() throws Exception {
    String html = mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    String src = Jsoup.parse(html).head().selectFirst("script[type=module][src^=/shadleaf/]").attr("src");
    assertThat(src).isEqualTo(new ShadleafAssets("default", AssetVariant.STANDALONE, AlpineVariant.CSP, null,
        manifestParser).getJsUrl());
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
