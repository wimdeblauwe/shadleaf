package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The default mode: every response carries a strict policy with a fresh nonce, and the theme script carries the same
 * nonce. Nothing else on the page needs an inline allowance.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CspNonceTest {

  private static final Pattern NONCE = Pattern.compile("'nonce-([A-Za-z0-9+/=]+)'");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ShadleafThemeScript themeScript;

  @ParameterizedTest
  @ValueSource(strings = {"/login", "/"})
  void themeScriptCarriesTheNonceFromThePolicy(String path) throws Exception {
    MockHttpServletResponse response = perform(get(path));
    String nonce = nonce(response);

    Element script = Jsoup.parse(response.getContentAsString()).head().selectFirst("script:not([src])");
    assertThat(script).isNotNull();
    assertThat(script.attr("nonce")).isEqualTo(nonce);
    assertThat(script.data()).isEqualTo(themeScript.getContent());
  }

  @Test
  void policyIsStrict() throws Exception {
    MockHttpServletResponse response = perform(get("/login"));

    assertThat(response.getHeader("Content-Security-Policy"))
        .isEqualTo("default-src 'self'; script-src 'self' 'nonce-%s'; style-src 'self'; img-src 'self'; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'", nonce(response))
        .doesNotContain("unsafe-inline", "unsafe-eval", "*", "data:");
  }

  @Test
  void everyRequestGetsItsOwnNonce() throws Exception {
    assertThat(nonce(perform(get("/login")))).isNotEqualTo(nonce(perform(get("/login"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/login", "/"})
  void pageNeedsNoInlineAllowance(String path) throws Exception {
    Document page = Jsoup.parse(perform(get(path)).getContentAsString());

    assertThat(page.select("script:not([src])")).as("inline scripts").hasSize(1);
    assertThat(page.select("script[src]")).allSatisfy(script -> assertThat(script.attr("src")).startsWith("/"));
    assertThat(page.select("style")).as("<style> elements").isEmpty();
    assertThat(page.select("[style]")).as("style attributes").isEmpty();
    assertThat(page.getAllElements()).allSatisfy(element -> assertThat(element.attributes().asList())
        .as("inline event handlers on <%s>", element.tagName())
        .noneMatch(attribute -> attribute.getKey().startsWith("on")));
    assertThat(page.select(".btn")).as("the page uses Shadleaf buttons").isNotEmpty();
  }

  @Test
  void pageNeedsSigningIn() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login"));
  }

  private MockHttpServletResponse perform(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request.with(user("user")))
        .andExpect(status().isOk())
        .andReturn().getResponse();
  }

  private static String nonce(MockHttpServletResponse response) {
    String policy = response.getHeader("Content-Security-Policy");
    assertThat(policy).isNotNull();
    Matcher matcher = NONCE.matcher(policy);
    assertThat(matcher.find()).as("nonce in %s", policy).isTrue();
    return matcher.group(1);
  }
}
