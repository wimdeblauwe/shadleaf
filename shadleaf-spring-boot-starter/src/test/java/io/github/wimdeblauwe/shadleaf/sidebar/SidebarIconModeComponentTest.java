package io.github.wimdeblauwe.shadleaf.sidebar;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.junit.jupiter.api.Test;

/**
 * Collapsing to icons and collapsible groups: the sidebar's {@code collapsible}, the menu button's {@code tooltip},
 * collapsible menu items and the sub-menu parts.
 */
class SidebarIconModeComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.builder().requestUri("/settings/billing").build();

  @Test
  void collapsibleIsRenderedUnlessOffcanvas() {
    assertThat(tester.render("<sl:sidebar>x</sl:sidebar>")).root().hasNoAttribute("data-collapsible");
    assertThat(tester.render("<sl:sidebar collapsible=\"icon\">x</sl:sidebar>")).root()
        .hasAttribute("data-collapsible", "icon");
    assertThat(tester.render("<sl:sidebar collapsible=\"none\">x</sl:sidebar>")).root()
        .hasAttribute("data-collapsible", "none");
  }

  @Test
  void theTooltipRepeatsTheLabelInsideTheButtonHiddenFromAssistiveTechnology() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item id="inbox">
          <sl:sidebar-menu-button href="/inbox" tooltip="Inbox">
            <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
            Inbox
          </sl:sidebar-menu-button>
          <sl:sidebar-menu-badge>12</sl:sidebar-menu-badge>
        </sl:sidebar-menu-item>""");

    assertThat(rendered).hasNoLeakedMarkup();
    // The button's name is its label; the tooltip adds no description, the badge still does.
    assertThat(rendered).element("a.sidebar-menu-button").hasAttribute("aria-describedby", "inbox-badge");
    assertThat(rendered).element("a.sidebar-menu-button > .sidebar-menu-button-label").hasText("Inbox");
    assertThat(rendered).element("a.sidebar-menu-button > span.tooltip-content.sidebar-menu-tooltip")
        .hasText("Inbox")
        .hasAttribute("popover", "manual")
        .hasAttribute("role", "tooltip")
        .hasAttribute("aria-hidden", "true")
        .hasAttribute("data-side", "right")
        .hasAttribute("x-data", "slSidebarMenuTooltip")
        .hasNoAttribute("id");
  }

  @Test
  void withoutATooltipPropThereIsNone() {
    Rendered rendered = tester.render("<sl:sidebar-menu-button href=\"/\">Home</sl:sidebar-menu-button>");

    assertThat(rendered).elements(".tooltip-content").isEmpty();
  }

  @Test
  void aCollapsibleItemIsDetailsWithTheButtonAsItsSummary() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu>
          <sl:sidebar-menu-item collapsible th:open="${#slNav.current('/settings')}">
            <sl:sidebar-menu-button tooltip="Settings">
              <sl:slot name="icon-start"><sl:icon name="settings"/></sl:slot>
              Settings
            </sl:sidebar-menu-button>
            <sl:sidebar-menu-sub>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button href="/settings" th:active="${#slNav.current('/settings', true)}">
                  General
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-item>
                <sl:sidebar-menu-sub-button href="/settings/billing"
                                            th:active="${#slNav.current('/settings/billing')}">
                  Billing
                </sl:sidebar-menu-sub-button>
              </sl:sidebar-menu-sub-item>
            </sl:sidebar-menu-sub>
          </sl:sidebar-menu-item>
        </sl:sidebar-menu>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("li.sidebar-menu-item > details.sidebar-menu-collapsible")
        .hasAttribute("open", "open");
    assertThat(rendered).element("details > summary.sidebar-menu-button:first-child")
        .hasNoAttribute("href", "type", "aria-current");
    assertThat(rendered).element("summary > svg.sidebar-menu-button-chevron");
    assertThat(rendered).element("summary > .tooltip-content").hasText("Settings");
    assertThat(rendered).element("details > ul.sidebar-menu-sub > li.sidebar-menu-sub-item");
    assertThat(rendered).elements("a.sidebar-menu-sub-button").hasSize(2);
    assertThat(rendered).element("a.sidebar-menu-sub-button[href='/settings']").hasNoAttribute("aria-current");
    assertThat(rendered).element("a.sidebar-menu-sub-button[href='/settings/billing']")
        .hasAttribute("aria-current", "page");
  }

  @Test
  void aClosedCollapsibleItemHasNoOpenAttribute() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item collapsible>
          <sl:sidebar-menu-button>Projects</sl:sidebar-menu-button>
        </sl:sidebar-menu-item>""");

    assertThat(rendered).element("details.sidebar-menu-collapsible").hasNoAttribute("open");
  }

  @Test
  void aButtonInAPlainItemNestedInACollapsibleOneIsNoSummary() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item collapsible>
          <sl:sidebar-menu-button>Projects</sl:sidebar-menu-button>
          <sl:sidebar-menu-sub>
            <sl:sidebar-menu-item><sl:sidebar-menu-button href="/p">Nested</sl:sidebar-menu-button></sl:sidebar-menu-item>
          </sl:sidebar-menu-sub>
        </sl:sidebar-menu-item>""");

    assertThat(rendered).elements("summary").hasSize(1);
    assertThat(rendered).element("ul.sidebar-menu-sub a.sidebar-menu-button").hasAttribute("href", "/p");
    assertThat(rendered).elements("svg.sidebar-menu-button-chevron").hasSize(1);
  }

  @Test
  void subButtonsAreLinksByDefault() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-sub-button href="/a" size="sm" class="extra" hx-boost="false">
          <sl:slot name="icon-start"><sl:icon name="file"/></sl:slot>
          Link
        </sl:sidebar-menu-sub-button>""");

    assertThat(rendered).root()
        .hasTag("a")
        .hasAttribute("class", "sidebar-menu-sub-button extra")
        .hasAttribute("href", "/a")
        .hasAttribute("hx-boost", "false")
        .hasAttribute("data-size", "sm");
    assertThat(rendered).element("a > svg + span.sidebar-menu-sub-button-label").hasText("Link");
  }

  @Test
  void aDisabledSubLinkLosesItsHref() {
    Rendered rendered = tester.render("<sl:sidebar-menu-sub-button href=\"/a\" disabled>Link</sl:sidebar-menu-sub-button>");

    assertThat(rendered).root()
        .hasNoAttribute("href")
        .hasAttribute("aria-disabled", "true")
        .hasAttribute("role", "link")
        .hasAttribute("tabindex", "-1");
  }

  @Test
  void anActiveSubButtonIsTheCurrentItem() {
    Rendered rendered = tester.render("<sl:sidebar-menu-sub-button as=\"button\" active>Drafts</sl:sidebar-menu-sub-button>");

    assertThat(rendered).root().hasTag("button").hasAttribute("type", "button").hasAttribute("aria-current", "true");
  }

  @Test
  void aCollapsibleGroupIsDetailsWithTheLabelAsItsSummary() {
    Rendered rendered = tester.render("""
        <sl:sidebar-group id="build" collapsible th:open="${#slNav.current('/build')}" class="extra">
          <sl:sidebar-group-label>Build</sl:sidebar-group-label>
          <sl:sidebar-group-content>
            <sl:sidebar-menu>
              <sl:sidebar-menu-item>
                <sl:sidebar-menu-button href="/build/routing">Routing</sl:sidebar-menu-button>
              </sl:sidebar-menu-item>
            </sl:sidebar-menu>
          </sl:sidebar-group-content>
        </sl:sidebar-group>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("div")
        .hasAttribute("class", "sidebar-group extra")
        .hasAttribute("role", "group")
        .hasAttribute("aria-labelledby", "build-label");
    assertThat(rendered).element("div.sidebar-group > details.sidebar-group-collapsible").hasNoAttribute("open");
    assertThat(rendered).element("details > summary.sidebar-group-label:first-child")
        .hasAttribute("id", "build-label")
        .hasText("Build");
    assertThat(rendered).element("summary > svg.sidebar-group-label-chevron");
    assertThat(rendered).element("details > .sidebar-group-content a.sidebar-menu-button");
  }

  @Test
  void aCollapsibleGroupRendersOpen() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/build/routing").build().render("""
        <sl:sidebar-group collapsible th:open="${#slNav.current('/build')}">
          <sl:sidebar-group-label>Build</sl:sidebar-group-label>
        </sl:sidebar-group>""");

    assertThat(rendered).element("details.sidebar-group-collapsible").hasAttribute("open", "open");
  }

  @Test
  void aLabelInAPlainGroupIsNoSummary() {
    Rendered rendered = tester.render("""
        <sl:sidebar-group id="platform"><sl:sidebar-group-label>Platform</sl:sidebar-group-label></sl:sidebar-group>""");

    assertThat(rendered).elements("details, summary").isEmpty();
    assertThat(rendered).element("div.sidebar-group > div.sidebar-group-label").hasAttribute("id", "platform-label");
    assertThat(rendered).elements("svg").isEmpty();
  }

  @Test
  void aCollapsibleSubMenuOpensBesideALink() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/playground/history").build().render("""
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button href="/playground">Playground</sl:sidebar-menu-button>
          <sl:sidebar-menu-sub collapsible th:open="${#slNav.current('/playground')}"
                               aria-label="Playground's pages" class="extra">
            <sl:sidebar-menu-sub-item>
              <sl:sidebar-menu-sub-button href="/playground/history"
                                          th:active="${#slNav.current('/playground/history')}">
                History
              </sl:sidebar-menu-sub-button>
            </sl:sidebar-menu-sub-item>
          </sl:sidebar-menu-sub>
        </sl:sidebar-menu-item>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("li > a.sidebar-menu-button + details.sidebar-menu-sub-collapsible")
        .hasAttribute("open", "open");
    assertThat(rendered).element("details > summary.sidebar-menu-action.sidebar-menu-sub-toggle:first-child")
        .hasAttribute("aria-label", "Playground's pages");
    assertThat(rendered).element("summary > svg.sidebar-menu-sub-toggle-icon");
    assertThat(rendered).element("details > ul.sidebar-menu-sub")
        .hasAttribute("class", "sidebar-menu-sub extra")
        .hasNoAttribute("aria-label");
    assertThat(rendered).element("a.sidebar-menu-sub-button").hasAttribute("aria-current", "page");
  }

  @Test
  void aCollapsibleSubMenuNeedsAName() {
    assertThatRenderFailure(() -> tester.render("<sl:sidebar-menu-sub collapsible>x</sl:sidebar-menu-sub>"))
        .hasMessageContaining("<sl:sidebar-menu-sub collapsible=\"true\">")
        .hasMessageContaining("accessible name");
    assertThat(tester.render("<sl:sidebar-menu-sub>x</sl:sidebar-menu-sub>")).root()
        .hasTag("ul")
        .hasAttribute("class", "sidebar-menu-sub");
    assertThat(tester.render("""
        <sl:sidebar-menu-sub collapsible th:aria-label="${'Pages'}">x</sl:sidebar-menu-sub>"""))
        .element("summary").hasAttribute("aria-label", "Pages");
  }
}
