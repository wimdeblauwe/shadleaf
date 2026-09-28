package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShadleafSample01ApplicationTest {

  private static final Pattern SHADLEAF_STYLESHEET =
      Pattern.compile("<link rel=\"stylesheet\" href=\"(/shadleaf/assets/shadleaf-default-[^\"]+\\.css)\"");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ShadleafThemeScript themeScript;

  @Test
  void pageLinksTheStylesheetFromTheShadleafJar() throws Exception {
    String html = mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    Matcher matcher = SHADLEAF_STYLESHEET.matcher(html);
    assertThat(matcher.find()).as("Shadleaf stylesheet link in:%n%s", html).isTrue();

    mockMvc.perform(get(matcher.group(1)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(".btn[data-variant=\"destructive\"]")))
        .andExpect(content().string(containsString("--primary:")));
  }

  @Test
  void themeScriptRunsBeforeTheStylesheetAndCarriesTheRequestNonce() throws Exception {
    String html = mockMvc.perform(get("/").requestAttr("cspNonce", "r4nd0m"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    Element head = Jsoup.parse(html).head();
    Element script = head.selectFirst("script[nonce]");
    assertThat(script).as("theme script in:%n%s", head).isNotNull();
    assertThat(script.attr("nonce")).isEqualTo("r4nd0m");
    assertThat(script.data()).isEqualTo(themeScript.getContent());
    assertThat(script.elementSiblingIndex())
        .isLessThan(head.selectFirst("link[href^=/shadleaf/]").elementSiblingIndex());
  }
}
