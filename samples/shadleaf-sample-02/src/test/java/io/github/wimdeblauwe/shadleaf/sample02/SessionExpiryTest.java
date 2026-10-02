package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * An htmx request after the session expired (here: without a session) is sent to its page with {@code HX-Redirect},
 * a normal navigation the login form then answers, instead of getting the sign-in page swapped into its target; and the
 * page, not the htmx request's URL, is where signing in returns to.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionExpiryTest {

  private static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theLazyTabIsSentToItsPage() throws Exception {
    MvcResult result = mockMvc.perform(get("/data/about")
            .header("HX-Request", "true")
            .header("HX-Current-URL", "http://localhost/data"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("HX-Redirect", "/data"))
        .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
        .andReturn();
    assertThat(result.getResponse().getContentAsString()).doesNotContain("Sign in");
    assertThat(savedRequest(result.getRequest().getSession(false))).isNull();
  }

  @Test
  void aBoostedLinkIsSentToThePageItLinksTo() throws Exception {
    mockMvc.perform(get("/data").queryParam("tab", "about")
            .header("HX-Request", "true")
            .header("HX-Boosted", "true")
            .header("HX-Current-URL", "http://localhost/forms"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("HX-Redirect", "/data?tab=about"));
  }

  @Test
  void aBoostedFormWhoseSessionExpiredIsSentToItsPage() throws Exception {
    // The form still holds the token of the session that expired; the server has none to compare it with.
    mockMvc.perform(post("/forms")
            .param("_csrf", "token-of-the-expired-session")
            .param("name", "Grace Hopper")
            .header("HX-Request", "true")
            .header("HX-Boosted", "true")
            .header("HX-Current-URL", "http://localhost/forms"))
        .andExpect(status().isForbidden())
        .andExpect(header().string("HX-Redirect", "/forms"));
  }

  @Test
  void signingInReturnsToThePageNotToTheFragment() throws Exception {
    MockHttpSession session = new MockHttpSession();

    mockMvc.perform(get("/data/about").session(session)
            .header("HX-Request", "true")
            .header("HX-Current-URL", "http://localhost/data"))
        .andExpect(header().string("HX-Redirect", "/data"));

    // The browser follows HX-Redirect: a normal navigation, which the login form answers.
    mockMvc.perform(get("/data").session(session).accept(MediaType.TEXT_HTML))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login"));

    mockMvc.perform(post("/login").session(session)
            .param("username", "grace")
            .param("password", "password")
            .with(csrf()))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("http://localhost/data?continue"));
  }

  @Test
  void signedInTheLazyTabGetsItsPanel() throws Exception {
    String html = mockMvc.perform(get("/data/about").with(user("grace").roles("USER"))
            .header("HX-Request", "true")
            .header("HX-Current-URL", "http://localhost/data"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    Document fragment = Jsoup.parseBodyFragment(html);
    assertThat(fragment.select("header, nav, h1")).isEmpty();
    assertThat(fragment.text()).contains("loaded with htmx the first time it is shown");
  }

  @Test
  void theServerRendersTheAboutPanelWhenItIsTheActiveTab() throws Exception {
    String html = mockMvc.perform(get("/data").queryParam("tab", "about").with(user("grace").roles("USER")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    Document page = Jsoup.parse(html);
    assertThat(page.selectFirst("#data-about-content").hasAttr("hx-get")).isFalse();
    assertThat(page.selectFirst("#data-about-content").text()).contains("/data/about");
    assertThat(page.selectFirst("#data-projects-content").attr("hx-get")).isEmpty();
  }

  @Test
  void otherwiseTheAboutPanelIsLoadedWithHtmx() throws Exception {
    String html = mockMvc.perform(get("/data").with(user("grace").roles("USER")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    Document page = Jsoup.parse(html);
    assertThat(page.selectFirst("#data-about-content").attr("hx-get")).isEqualTo("/data/about");
    assertThat(page.selectFirst("#data-about-content").attr("hx-trigger")).isEqualTo("sl-tabs-show once");
    assertThat(page.body().attr("hx-boost")).isEqualTo("true");
    assertThat(page.select("script[src^=/webjars/htmx.org/]")).hasSize(1);
  }

  @Test
  void aUserWithoutTheRoleStillGetsAPlainForbidden() throws Exception {
    mockMvc.perform(get("/admin").with(user("grace").roles("USER"))
            .header("HX-Request", "true")
            .header("HX-Boosted", "true")
            .header("HX-Current-URL", "http://localhost/data"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("HX-Redirect"))
        .andExpect(header().doesNotExist("HX-Refresh"));
  }

  @Test
  void aRequestThatIsNoHtmxRequestStillGoesToTheSignInPage() throws Exception {
    mockMvc.perform(get("/data"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login"))
        .andExpect(header().doesNotExist("HX-Redirect"));
  }

  @Test
  void theHtmxScriptIsServedWithoutSigningIn() throws Exception {
    String html = mockMvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
    String src = Jsoup.parse(html).selectFirst("script[src^=/webjars/htmx.org/]").attr("src");
    mockMvc.perform(get(src)).andExpect(status().isOk());
  }

  private static Object savedRequest(HttpSession session) {
    return session == null ? null : session.getAttribute(SAVED_REQUEST);
  }
}
