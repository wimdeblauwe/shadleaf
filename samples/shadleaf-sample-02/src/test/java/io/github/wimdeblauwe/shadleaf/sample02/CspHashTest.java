package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The hash mode, for a policy that cannot change per request: the header carries the hash the library publishes,
 * and that hash matches the script as the page serves it.
 */
@SpringBootTest(properties = "sample.csp.mode=hash")
@AutoConfigureMockMvc
class CspHashTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ShadleafThemeScript themeScript;

  @Test
  void publishedHashAllowsTheServedScript() throws Exception {
    MockHttpServletResponse response = mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andReturn().getResponse();

    Element script = Jsoup.parse(response.getContentAsString()).head().selectFirst("script:not([src])");
    assertThat(script).isNotNull();
    assertThat(script.hasAttr("nonce")).isFalse();

    String servedHash = "'sha256-" + sha256Base64(script.data()) + "'";
    assertThat(themeScript.getCspHash()).isEqualTo(servedHash);
    assertThat(response.getHeader("Content-Security-Policy"))
        .contains("script-src 'self' " + servedHash + ";")
        .doesNotContain("nonce-", "unsafe-inline");
  }

  private static String sha256Base64(String value) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(digest);
  }
}
