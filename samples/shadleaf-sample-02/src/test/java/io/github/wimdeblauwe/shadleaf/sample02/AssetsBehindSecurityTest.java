package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.assets.*;

import java.util.ArrayList;
import java.util.List;
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
  // And loads one on demand, as the chart loader does: import(`./sl-chart-C9x2kR1a.js`)
  private static final Pattern DYNAMIC_IMPORT = Pattern.compile("import\\(`\\./([^`]+\\.js)`");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ViteManifestParser manifestParser;

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource({
      "VEGA,    STANDALONE",
      "VEGA,    EMBEDDED",
      "LYRA,    STANDALONE",
      "LYRA,    EMBEDDED"
  })
  void everyBundleIsServedAnonymously(Skin skin, AssetVariant variant) throws Exception {
    String cssUrl = new ShadleafAssets(skin, variant, AlpineVariant.BUNDLED, null, manifestParser).getCssUrl();

    assertThat(cssUrl).startsWith("/shadleaf/");
    mockMvc.perform(get(cssUrl))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/css")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(".button")));
  }

  @ParameterizedTest
  @EnumSource(AlpineVariant.class)
  void everyScriptAndTheChunksItImportsAreServedAnonymously(AlpineVariant alpine) throws Exception {
    String jsUrl = new ShadleafAssets(Skin.VEGA, AssetVariant.STANDALONE, alpine, null, manifestParser).getJsUrl();

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

  @ParameterizedTest
  @EnumSource(AlpineVariant.class)
  void theChartChunkEveryScriptLoadsOnDemandIsServedAnonymously(AlpineVariant alpine) throws Exception {
    String jsUrl = new ShadleafAssets(Skin.VEGA, AssetVariant.STANDALONE, alpine, null, manifestParser).getJsUrl();
    String base = jsUrl.substring(0, jsUrl.lastIndexOf('/') + 1);
    List<String> chunks = new ArrayList<>();
    Matcher imports = RELATIVE_IMPORT.matcher(mockMvc.perform(get(jsUrl)).andReturn().getResponse()
        .getContentAsString());
    while (imports.find()) {
      Matcher dynamicImports = DYNAMIC_IMPORT.matcher(mockMvc.perform(get(base + imports.group(1))).andReturn()
          .getResponse().getContentAsString());
      while (dynamicImports.find()) {
        chunks.add(dynamicImports.group(1));
      }
    }

    assertThat(chunks).singleElement().asString().startsWith("sl-chart-");
    mockMvc.perform(get(base + chunks.get(0)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/javascript")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("sl-chart")));
  }

  @Test
  void theLoginPageLinksTheConfiguredBundle() throws Exception {
    String html = mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    String href = Jsoup.parse(html).head().selectFirst("link[href^=/shadleaf/]").attr("href");
    assertThat(href).isEqualTo(new ShadleafAssets(Skin.VEGA, AssetVariant.STANDALONE, AlpineVariant.CSP, null,
        manifestParser).getCssUrl());
    mockMvc.perform(get(href)).andExpect(status().isOk());
  }

  @Test
  void theLoginPageLoadsAlpinesCspBuild() throws Exception {
    String html = mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    String src = Jsoup.parse(html).head().selectFirst("script[type=module][src^=/shadleaf/]").attr("src");
    assertThat(src).isEqualTo(new ShadleafAssets(Skin.VEGA, AssetVariant.STANDALONE, AlpineVariant.CSP, null,
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
