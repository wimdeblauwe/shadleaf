package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The layout every page shares: shadcn/ui's sidebar-07 with the inset variant, collapsing to icons. A skip link first,
 * the sidebar (the one navigation landmark besides the breadcrumb) with the current page marked by
 * {@code #slNav.current} and the people pages in a collapsible group that is open on them, the inset (the
 * one {@code main}) with the trigger, a separator, the page's breadcrumb and the theme toggle, and the body boosted by
 * htmx. The server renders the sidebar's state from its cookie. A link the boost sends gets the whole page, also from
 * the controllers that answer other htmx requests with a fragment.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShellPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PersonRepository repository;

  /** Every page: its URL, the menu button or sub-button marked as current, and the breadcrumb's levels. */
  static Stream<Arguments> pages() {
    return Stream.of(
        Arguments.of("/", "/", List.of("Basics", "Buttons")),
        Arguments.of("/form", "/form", List.of("Forms", "Form")),
        Arguments.of("/htmx-form", "/htmx-form", List.of("Forms", "Form with htmx")),
        Arguments.of("/dialog", "/dialog", List.of("Interactive", "Dialog")),
        Arguments.of("/settings", "/settings", List.of("Interactive", "Settings")),
        Arguments.of("/settings?tab=notifications", "/settings", List.of("Interactive", "Settings")),
        Arguments.of("/people", "/people", List.of("Tables", "People")),
        Arguments.of("/people?q=ada&sort=email,desc&page=1", "/people", List.of("Tables", "People")),
        Arguments.of("/people-multiselect", "/people-multiselect", List.of("Tables", "People with multiselect")),
        Arguments.of("/people-load-more", "/people-load-more", List.of("Tables", "People, load more")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("pages")
  void everyPageHasTheShell(String url, String current, List<String> levels) throws Exception {
    Document page = page(get(url));

    assertShell(page);
    assertThat(currentMenuButtons(page)).containsExactly(current);
    // The people group is open exactly when it holds the current page.
    assertThat(page.selectFirst("nav.sidebar details.sidebar-menu-collapsible").hasAttr("open"))
        .isEqualTo(current.startsWith("/people"));
    assertThat(page.select(".breadcrumb-item").eachText()).isEqualTo(levels);
    assertThat(page.select(".breadcrumb-page[aria-current=page]").text()).isEqualTo(levels.get(levels.size() - 1));
    assertThat(page.select("main h1").text()).isEqualTo(levels.get(levels.size() - 1));
    assertThat(page.title()).isEqualTo(levels.get(levels.size() - 1) + " - Shadleaf Sample App 1");
    assertIdsUnique(page);
  }

  @Test
  void aPersonsPageMarksThePeoplePageAndLinksBackToIt() throws Exception {
    Person person = repository.findAll(Sort.by("id")).get(0);
    Document page = page(get("/people/{id}", person.getId()));

    assertShell(page);
    assertThat(currentMenuButtons(page)).containsExactly("/people");
    assertThat(page.selectFirst("nav.sidebar details.sidebar-menu-collapsible").hasAttr("open")).isTrue();
    assertThat(page.select(".breadcrumb-item").eachText()).containsExactly("Tables", "People", person.getName());
    assertThat(page.select(".breadcrumb-link").attr("href")).isEqualTo("/people");
    assertThat(page.select(".breadcrumb-page").text()).isEqualTo(person.getName());
    assertIdsUnique(page);
  }

  @Test
  void theSidebarIsExpandedUntilTheCookieSaysOtherwise() throws Exception {
    assertThat(page(get("/")).selectFirst(".sidebar-provider").attr("data-state")).isEqualTo("expanded");
    assertThat(page(get("/people").cookie(new Cookie("sl-sidebar-state", "collapsed")))
        .selectFirst(".sidebar-provider").attr("data-state")).isEqualTo("collapsed");
    assertThat(page(get("/people").cookie(new Cookie("sl-sidebar-state", "expanded")))
        .selectFirst(".sidebar-provider").attr("data-state")).isEqualTo("expanded");
  }

  @Test
  void theHeaderHoldsTheTriggerSeparatorBreadcrumbAndThemeToggle() throws Exception {
    Element header = page(get("/form")).selectFirst("main#main > header.inset-header");

    assertThat(header).isNotNull();
    assertThat(header.children().stream().map(Element::className))
        .containsExactly("btn sidebar-trigger", "separator", "breadcrumb", "inset-header-end");
    Element trigger = header.selectFirst(".sidebar-trigger");
    assertThat(trigger.attr("popovertarget")).isEqualTo("sidebar");
    assertThat(trigger.attr("aria-label")).isEqualTo("Toggle sidebar");
    assertThat(header.selectFirst(".separator").attr("data-orientation")).isEqualTo("vertical");
    assertThat(header.select(".inset-header-end .theme-toggle")).hasSize(1);
  }

  @Test
  void theSidebarHasTheApplicationGroupsAndTheDocumentation() throws Exception {
    Element sidebar = page(get("/")).selectFirst("nav.sidebar");

    assertThat(sidebar.select(".sidebar-header .sidebar-menu-button").attr("href")).isEqualTo("/");
    assertThat(sidebar.select(".sidebar-header .sidebar-menu-button").attr("data-size")).isEqualTo("lg");
    assertThat(sidebar.select(".sidebar-header .sidebar-menu-button-label > *").eachText())
        .containsExactly("Shadleaf", "Sample app 1");
    // The labels (a menu button's text also holds its tooltip).
    Map<String, List<String>> groups = sidebar.select(".sidebar-content > .sidebar-group").stream()
        .collect(Collectors.toMap(group -> group.selectFirst(".sidebar-group-label").text(),
            group -> group.select(".sidebar-menu-button-label, .sidebar-menu-sub-button-label").eachText(),
            (a, b) -> a, LinkedHashMap::new));
    assertThat(groups).containsExactly(
        Map.entry("Basics", List.of("Buttons")),
        Map.entry("Forms", List.of("Form", "Form with htmx")),
        Map.entry("Interactive", List.of("Dialog", "Settings")),
        Map.entry("Tables", List.of("People", "All people", "With multiselect", "Load more")));
    // People is a collapsible group: its button is the summary, the three pages its sub-menu.
    Element people = sidebar.selectFirst("details.sidebar-menu-collapsible");
    assertThat(people.child(0).tagName()).isEqualTo("summary");
    assertThat(people.select(".sidebar-menu-sub-button").eachAttr("href"))
        .containsExactly("/people", "/people-multiselect", "/people-load-more");
    // Collapsed to icons, every menu button shows its label as a tooltip, which repeats its name.
    assertThat(sidebar.attr("data-collapsible")).isEqualTo("icon");
    for (Element button : sidebar.select(".sidebar-menu-button")) {
      Element tooltip = button.selectFirst("> .sidebar-menu-tooltip");
      assertThat(tooltip).as("tooltip of %s", button.text()).isNotNull();
      assertThat(tooltip.attr("aria-hidden")).isEqualTo("true");
      assertThat(tooltip.ownText()).isEqualTo(button.selectFirst(".sidebar-menu-button-label").text()
          .replace("Shadleaf Sample app 1", "Shadleaf"));
    }
    // Each group is named by its label.
    for (Element group : sidebar.select(".sidebar-content > .sidebar-group")) {
      assertThat(group.attr("role")).isEqualTo("group");
      assertThat(group.attr("aria-labelledby")).isEqualTo(group.selectFirst(".sidebar-group-label").id());
    }
    assertThat(sidebar.select(".sidebar-footer .sidebar-menu-button-label").eachText()).containsExactly("Documentation");
  }

  /** Requests a link in the boosted body sends: HX-Request and HX-Boosted, no HX-Target (the body has no id). */
  static Stream<String> boostedPages() {
    return Stream.of("/", "/form", "/htmx-form", "/dialog", "/settings", "/people", "/people-multiselect",
        "/people-load-more");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("boostedPages")
  void aLinkTheBoostSendsGetsTheWholePage(String url) throws Exception {
    Document page = page(get(url).header("HX-Request", "true").header("HX-Boosted", "true")
        .header("HX-Current-URL", "http://localhost/"));

    assertShell(page);
  }

  @Test
  void aPersonsDeletePageIsWholeWhenBoostedAndTheDialogWhenTargeted() throws Exception {
    Person person = repository.findAll(Sort.by("id")).get(0);

    assertShell(page(get("/people/{id}/delete", person.getId())
        .header("HX-Request", "true").header("HX-Boosted", "true")));
    String dialog = html(get("/people/{id}/delete", person.getId())
        .header("HX-Request", "true").header("HX-Boosted", "true").header("HX-Target", "modal-root"));
    assertThat(dialog).contains("alert-dialog").doesNotContain("sidebar-provider");
  }

  @Test
  void aBoostedLinkInsideTheResultsStillGetsTheResultsAlone() throws Exception {
    String fragment = html(get("/people?sort=email,asc")
        .header("HX-Request", "true").header("HX-Boosted", "true").header("HX-Target", "people-results"));

    assertThat(fragment).contains("id=\"people-results\"").doesNotContain("sidebar-provider", "<h1");
  }

  private static void assertShell(Document page) {
    Element body = page.body();
    assertThat(body.attr("hx-boost")).isEqualTo("true");
    // The skip link comes first, to the inset.
    Element skipLink = body.child(0);
    assertThat(skipLink.className()).isEqualTo("skip-link");
    assertThat(skipLink.attr("href")).isEqualTo("#main");
    // Outside the page's content: one navigation landmark for the site (the sidebar), one for the breadcrumb.
    assertThat(page.select("nav").stream()
        .filter(nav -> nav.parents().stream().noneMatch(parent -> parent.hasClass("page")))
        .map(nav -> nav.attr("aria-label")))
        .containsExactly("Main", "Breadcrumb");
    Element sidebar = page.selectFirst("nav.sidebar");
    assertThat(sidebar.id()).isEqualTo("sidebar");
    assertThat(sidebar.hasAttr("popover")).isTrue();
    assertThat(sidebar.attr("data-variant")).isEqualTo("inset");
    assertThat(page.select("main")).hasSize(1);
    Element main = page.selectFirst("main");
    assertThat(main.id()).isEqualTo("main");
    assertThat(main.attr("tabindex")).isEqualTo("-1");
    assertThat(main.parent()).isEqualTo(sidebar.parent());
    assertThat(sidebar.parent().className()).isEqualTo("sidebar-provider");
    // The modal root and the toaster stay where they were, after the shell.
    assertThat(page.select("body > #modal-root")).hasSize(1);
    assertThat(page.select("body > .toaster")).hasSize(1);
  }

  /** The hrefs of the menu buttons and sub-buttons marked as the current page. */
  private static List<String> currentMenuButtons(Document page) {
    return page.select("nav.sidebar :is(.sidebar-menu-button, .sidebar-menu-sub-button)[aria-current]").stream()
        .peek(button -> assertThat(button.attr("aria-current")).isEqualTo("page"))
        .map(button -> button.attr("href"))
        .toList();
  }

  /** No id twice, toasts in the toaster's templates aside (a template's content is no part of the page). */
  private static void assertIdsUnique(Document page) {
    Map<String, Long> counts = page.select("[id]").stream()
        .filter(element -> element.parents().stream().noneMatch(parent -> parent.tagName().equals("template")))
        .collect(Collectors.groupingBy(Element::id, Collectors.counting()));
    assertThat(counts).allSatisfy((id, count) -> assertThat(count).as("elements with id %s", id).isEqualTo(1));
  }

  private Document page(MockHttpServletRequestBuilder request) throws Exception {
    return Jsoup.parse(html(request));
  }

  private String html(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  }
}
