package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The header layout on every signed-in page, under the strict policy: one navigation in the site header with
 * placement="header" (a row of links from 768 px, the panel on a phone), the current page marked, Settings and Help only
 * in the panel, the theme toggle and Sign out at the end, the page in the inset; and nothing that needs an inline
 * allowance. The sign-in page has the header without the navigation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HeaderLayoutPageTest {

  @Autowired
  private MockMvc mockMvc;

  @ParameterizedTest
  @CsvSource({
      "/, Home, Home",
      "/forms, Forms, Forms",
      "/overlays, Overlays, Overlays",
      "/data, Data, Data",
      "/data?tab=about, Data, Data",
      "/settings, Settings, Settings",
      "/help, Help, Help",
  })
  void everyPageHasTheHeaderLayout(String path, String title, String current) throws Exception {
    Document page = page(path);

    assertThat(page.title()).isEqualTo(title + " - Shadleaf Sample App 2");
    Element skipLink = page.body().child(0);
    assertThat(skipLink.hasClass("skip-link")).isTrue();
    assertThat(skipLink.attr("href")).isEqualTo("#main");
    Element header = page.selectFirst("body > .sidebar-provider > header.site-header");
    assertThat(header).isNotNull();
    assertThat(page.select("nav")).hasSize(1);
    Element nav = header.selectFirst("> nav#sidebar.sidebar");
    assertThat(nav).isNotNull();
    assertThat(nav.attr("data-placement")).isEqualTo("header");
    assertThat(nav.attr("data-variant")).isEqualTo("inset");
    assertThat(nav.hasAttr("popover")).isTrue();
    assertThat(header.selectFirst("> .sidebar-trigger").attr("popovertarget")).isEqualTo("sidebar");
    assertThat(header.select("> .site-header-end .theme-toggle")).hasSize(1);
    assertThat(header.selectFirst("> .site-header-end form[action=/logout] input[name=_csrf]")).isNotNull();

    assertThat(nav.select(".sidebar-menu-button[aria-current=page] .sidebar-menu-button-label"))
        .extracting(Element::text).containsExactly(current);
    assertThat(nav.select(".sidebar-content > .sidebar-group:not([data-panel-only]) .sidebar-menu-button-label"))
        .extracting(Element::text).containsExactly("Home", "Forms", "Overlays", "Data");
    assertThat(nav.select("[data-panel-only] .sidebar-menu-button-label"))
        .extracting(Element::text).containsExactly("Shadleaf Sample App 2", "Settings", "Help");

    Element main = page.selectFirst(".sidebar-provider > main#main.sidebar-inset");
    assertThat(main).isNotNull();
    assertThat(main.selectFirst("h1").text()).isEqualTo(title);
    assertThat(page.select("body > .toaster")).hasSize(1);
    assertUniqueIds(page);
    assertNoInlineAllowanceNeeded(page);
  }

  @Test
  void theSignInPageHasTheHeaderWithoutTheNavigation() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/login")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    assertThat(page.select("body > header.site-header")).hasSize(1);
    assertThat(page.select("nav, .sidebar-trigger")).isEmpty();
    assertThat(page.select("body > header.site-header .theme-toggle")).hasSize(1);
    assertThat(page.selectFirst("body > main#main h1").text()).isEqualTo("Sign in");
    assertUniqueIds(page);
    assertNoInlineAllowanceNeeded(page);
  }

  @Test
  void theFormShowsAnErrorAndSavesWithAToast() throws Exception {
    String invalid = mockMvc.perform(post("/forms").with(user("user")).with(csrf())
            .param("name", "").param("email", "").param("plan", "team"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(Jsoup.parse(invalid).selectFirst("#name").attr("aria-invalid")).isEqualTo("true");

    mockMvc.perform(post("/forms").with(user("user")).with(csrf())
            .param("name", "Ada").param("email", "ada@example.com").param("plan", "team"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/forms"))
        .andExpect(flash().attributeExists("toasts"));
  }

  private Document page(String path) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(path).with(user("user")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }

  /** Ids outside templates (the toaster's toast templates hold none, but a template's content is not in the page). */
  private static void assertUniqueIds(Document page) {
    Set<String> seen = new HashSet<>();
    List<String> duplicates = page.select("[id]").stream()
        .filter(element -> element.parents().stream().noneMatch(parent -> parent.tagName().equals("template")))
        .map(element -> element.id())
        .filter(id -> !seen.add(id))
        .toList();
    assertThat(duplicates).as("duplicate ids").isEmpty();
  }

  private static void assertNoInlineAllowanceNeeded(Document page) {
    assertThat(page.select("script:not([src])")).as("inline scripts: only the theme script").hasSize(1);
    assertThat(page.select("style")).as("<style> elements").isEmpty();
    assertThat(page.select("[style]")).as("style attributes").isEmpty();
    assertThat(page.getAllElements()).allSatisfy(element -> assertThat(element.attributes().asList())
        .as("inline event handlers on <%s>", element.tagName())
        .noneMatch(attribute -> attribute.getKey().startsWith("on")));
  }
}
