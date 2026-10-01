package io.github.wimdeblauwe.shadleaf.sidebar;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SidebarComponentTest {

  private static final String PROVIDER = "<sl:sidebar-provider>x</sl:sidebar-provider>";

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  private static ComponentRenderTester withCookie(String value) {
    return ComponentRenderTester.builder().cookie(SidebarState.COOKIE_NAME, value).build();
  }

  @Test
  void theProviderHoldsTheStateForSlSidebar() {
    Rendered rendered = tester.render(PROVIDER);

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("div")
        .hasClassName("sidebar-provider")
        .hasAttribute("x-data", "slSidebar")
        .hasAttribute("data-state", "expanded");
  }

  @ParameterizedTest(name = "cookie {0} -> {1}")
  @CsvSource({
      "collapsed, collapsed",
      "expanded, expanded",
      // Anything else is ignored.
      "closed, expanded",
      "'', expanded"})
  void theCookieDecidesTheState(String cookie, String state) {
    assertThat(withCookie(cookie).render(PROVIDER)).root().hasAttribute("data-state", state);
  }

  @Test
  void theDefaultStateAppliesUntilThereIsACookie() {
    String provider = "<sl:sidebar-provider default-state=\"collapsed\">x</sl:sidebar-provider>";

    assertThat(tester.render(provider)).root().hasAttribute("data-state", "collapsed");
    assertThat(withCookie("expanded").render(provider)).root().hasAttribute("data-state", "expanded");
  }

  @Test
  void anExplicitStateWinsOverTheCookie() {
    assertThat(withCookie("collapsed").render("<sl:sidebar-provider state=\"expanded\">x</sl:sidebar-provider>"))
        .root().hasAttribute("data-state", "expanded");
    assertThat(withCookie("expanded").render("<sl:sidebar-provider th:state=\"${saved}\">x</sl:sidebar-provider>",
        Map.of("saved", "collapsed"))).root().hasAttribute("data-state", "collapsed");
  }

  @Test
  void aNullStateFallsBackToTheCookie() {
    assertThat(withCookie("collapsed").render("<sl:sidebar-provider th:state=\"${null}\">x</sl:sidebar-provider>"))
        .root().hasAttribute("data-state", "collapsed");
  }

  @Test
  void theSidebarIsANavPopoverNamedMain() {
    Rendered rendered = tester.render("<sl:sidebar><a href=\"/\">Home</a></sl:sidebar>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("nav")
        .hasClassName("sidebar")
        .hasAttribute("id", "sidebar")
        .hasAttribute("popover", "")
        .hasAttribute("aria-label", "Main")
        .hasNoAttribute("data-side", "data-collapsible", "data-state", "role");
    assertThat(rendered).element("nav > a").hasText("Home");
  }

  @Test
  void theSidebarTakesAnIdASideAndItsOwnName() {
    assertThat(tester.render("<sl:sidebar id=\"docs-nav\" side=\"end\" aria-label=\"Documentation\">x</sl:sidebar>"))
        .root()
        .hasAttribute("id", "docs-nav")
        .hasAttribute("data-side", "end")
        .hasAttribute("aria-label", "Documentation");
  }

  @Test
  void theTriggerIsAnIconButtonThatOpensTheSidebarPanel() {
    Rendered rendered = tester.render("<sl:sidebar-trigger/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("button")
        .hasClass("btn", "sidebar-trigger")
        .hasAttribute("type", "button")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon-sm")
        .hasAttribute("popovertarget", "sidebar")
        .hasAttribute("aria-controls", "sidebar")
        .hasAttribute("aria-label", "Toggle sidebar")
        .hasNoAttribute("aria-expanded");
    assertThat(rendered).element("button > svg.sidebar-trigger-icon").hasAttribute("aria-hidden", "true");
  }

  @Test
  void theTriggerPointsAtAnotherSidebar() {
    assertThat(tester.render("<sl:sidebar-trigger for=\"docs-nav\"/>")).root()
        .hasAttribute("popovertarget", "docs-nav")
        .hasAttribute("aria-controls", "docs-nav");
  }

  @Test
  void anAriaLabelReplacesTheTriggersName() {
    assertThat(tester.render("<sl:sidebar-trigger aria-label=\"Menu\"/>")).root()
        .hasAttribute("aria-label", "Menu");
    assertThat(tester.render("<sl:sidebar-trigger th:aria-label=\"${label}\"/>", Map.of("label", "Navigation")))
        .root().hasAttribute("aria-label", "Navigation");
  }

  @Test
  void theTriggerPassesAttributesToTheButton() {
    Rendered rendered = tester.render(
        "<sl:sidebar-trigger id=\"menu-button\" class=\"me-2\" variant=\"outline\" size=\"icon\">"
        + "<sl:icon name=\"menu\"/></sl:sidebar-trigger>");

    assertThat(rendered).root()
        .hasAttribute("id", "menu-button")
        .hasClass("btn", "sidebar-trigger", "me-2")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("data-size", "icon");
    assertThat(rendered).root().hasNoElement(".sidebar-trigger-icon");
  }

  @Test
  void theInsetIsTheMainElement() {
    Rendered rendered = tester.render("<sl:sidebar-inset id=\"content\">Page</sl:sidebar-inset>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("main")
        .hasClassName("sidebar-inset")
        .hasAttribute("id", "content")
        .hasText("Page");
  }
}
