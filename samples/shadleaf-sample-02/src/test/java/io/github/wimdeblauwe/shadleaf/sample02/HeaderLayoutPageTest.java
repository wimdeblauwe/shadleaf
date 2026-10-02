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
import org.springframework.mock.web.MockHttpSession;
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
 * in the panel, Admin only for admins, the theme toggle and the user menu at the end, the page in the inset; and nothing
 * that needs an inline allowance. The sign-in page has the header without the navigation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HeaderLayoutPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SampleUsers users;

  @ParameterizedTest
  @CsvSource({
      "/, Home, Home, grace",
      "/forms, Forms, Forms, grace",
      "/overlays, Overlays, Overlays, grace",
      "/data, Data, Data, grace",
      "/data?tab=about, Data, Data, grace",
      "/settings, Settings, Settings, grace",
      "/help, Help, Help, grace",
      "/, Home, Home, ada",
      "/data, Data, Data, ada",
      "/admin, Admin, Admin, ada",
  })
  void everyPageHasTheHeaderLayout(String path, String title, String current, String username) throws Exception {
    Document page = page(path, username);
    boolean admin = username.equals("ada");

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
    // The user menu: the picture button (initials: the sample's users have no picture), Settings and Sign out, and the
    // form Sign out submits, outside the menu, with the CSRF token.
    Element userMenu = header.selectFirst("> .site-header-end > button#user-menu-trigger");
    assertThat(userMenu.attr("aria-label")).isEqualTo(admin ? "Account: Ada Lovelace" : "Account: Grace Hopper");
    assertThat(userMenu.selectFirst(".avatar-fallback").text()).isEqualTo(admin ? "AL" : "GH");
    assertThat(header.select("> .site-header-end > #user-menu[role=menu] [role=menuitem]").eachText())
        .containsExactly("Settings", "Sign out");
    Element signOut = header.selectFirst("> .site-header-end > form#user-menu-sign-out");
    assertThat(signOut.attr("action")).isEqualTo("/logout");
    assertThat(signOut.selectFirst("input[name=_csrf]")).isNotNull();

    assertThat(nav.select(".sidebar-menu-button[aria-current=page] .sidebar-menu-button-label"))
        .extracting(Element::text).containsExactly(current);
    assertThat(nav.select(".sidebar-content > .sidebar-group:not([data-panel-only]) .sidebar-menu-button-label"))
        .extracting(Element::text)
        .containsExactlyElementsOf(admin ? List.of("Home", "Forms", "Overlays", "Data", "Admin")
            : List.of("Home", "Forms", "Overlays", "Data"));
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

  /**
   * The user menu shows the name and email of the sample's {@link SampleUser}, through the application's
   * {@code CurrentUserResolver}: Spring Security's {@code UserDetails} has only a username.
   */
  @ParameterizedTest
  @CsvSource({
      "ada, Ada Lovelace, ada@example.com",
      "grace, Grace Hopper, grace@example.com",
  })
  void theUserMenuShowsTheUsersName(String username, String name, String email) throws Exception {
    Document page = page("/", username);

    assertThat(page.selectFirst("#user-menu .user-menu-name").text()).isEqualTo(name);
    assertThat(page.selectFirst("#user-menu .user-menu-detail").text()).isEqualTo(email);
    assertThat(page.selectFirst(".page strong").text()).isEqualTo(name);
  }

  /**
   * Navigation by role: the Admin link and Data's Manage access only for admins. There is one navigation, so the
   * phone panel follows the same rule as the header row: the link is in it or not, and not panel-only either way.
   */
  @ParameterizedTest
  @CsvSource({
      "ada, true",
      "grace, false",
  })
  void onlyAdminsSeeTheAdminLinkAndControl(String username, boolean admin) throws Exception {
    Document page = page("/data", username);

    Element nav = page.selectFirst("nav#sidebar[popover]");
    assertThat(page.select("nav")).hasSize(1);
    assertThat(nav.select(".sidebar-menu-button[href=/admin]")).hasSize(admin ? 1 : 0);
    assertThat(nav.select("[data-panel-only] [href=/admin]")).isEmpty();
    assertThat(page.select("#manage-access")).hasSize(admin ? 1 : 0);
    assertThat(page.select("a[href=/admin]")).hasSize(admin ? 2 : 0);
  }

  /** Hiding the link is no security; the URL rule is. spring-security-test's users carry only roles. */
  @Test
  void theAdminPageNeedsTheAdminRole() throws Exception {
    mockMvc.perform(get("/admin").with(user("grace").roles("USER")))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/admin/anything").with(user("grace").roles("USER")))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/admin").with(user("ada").roles("ADMIN", "USER")))
        .andExpect(status().isOk());
    mockMvc.perform(get("/admin"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login"));
  }

  /**
   * For real: sign in with the sign-in page's form, then sign out with the user menu's form and the token it holds, as
   * the browser sends them. The session is signed out afterwards, and a post without the token is refused.
   */
  @Test
  void theUserMenuSignsOutForReal() throws Exception {
    MockHttpSession session = new MockHttpSession();
    Document login = Jsoup.parse(mockMvc.perform(get("/login").session(session))
        .andReturn().getResponse().getContentAsString());
    mockMvc.perform(post("/login").session(session)
            .param("username", "ada").param("password", "password")
            .param("_csrf", login.selectFirst("form input[name=_csrf]").val()))
        .andExpect(redirectedUrl("/"));

    Document home = Jsoup.parse(mockMvc.perform(get("/").session(session))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    assertThat(home.selectFirst("#user-menu-trigger").attr("aria-label")).isEqualTo("Account: Ada Lovelace");
    assertThat(home.select("nav a[href=/admin]")).hasSize(1);
    Element form = home.selectFirst("form#user-menu-sign-out");
    assertThat(home.selectFirst(".user-menu-sign-out").attr("form")).isEqualTo(form.id());

    mockMvc.perform(post(form.attr("action")).session(session))
        .andExpect(status().isForbidden());
    mockMvc.perform(post(form.attr("action")).session(session)
            .param("_csrf", form.selectFirst("input[name=_csrf]").val()))
        .andExpect(redirectedUrl("/login?logout"));
    Document signedOut = Jsoup.parse(mockMvc.perform(get("/login?logout").session(session))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    assertThat(signedOut.selectFirst(".message[role=status]").text()).isEqualTo("You have been signed out.");
    mockMvc.perform(get("/").session(session))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login"));

    // Signing in again from the "signed out" page goes home, not back to that page.
    MockHttpSession again = new MockHttpSession();
    Document loginAgain = Jsoup.parse(mockMvc.perform(get("/login?logout").session(again))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    mockMvc.perform(post("/login").session(again)
            .param("username", "ada").param("password", "password")
            .param("_csrf", loginAgain.selectFirst("form input[name=_csrf]").val()))
        .andExpect(redirectedUrl("/"));
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

  /** The page as one of the sample's users, as signing in with the form would give it. */
  private Document page(String path, String username) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(path).with(user(users.loadUserByUsername(username))))
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
