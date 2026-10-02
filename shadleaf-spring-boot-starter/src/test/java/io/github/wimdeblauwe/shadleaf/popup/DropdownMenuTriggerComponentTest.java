package io.github.wimdeblauwe.shadleaf.popup;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:dropdown-menu-trigger as="...">}: the menu opened from a button, a sidebar menu button or a sidebar menu
 * action, with the part's props and named slots passed on.
 */
class DropdownMenuTriggerComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void aButtonPassesItsIconSlotsOn() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu id="sort">
          <sl:dropdown-menu-trigger variant="outline">
            <sl:slot name="icon-start"><sl:icon name="arrow-up-down"/></sl:slot>
            Sort
            <sl:slot name="icon-end"><sl:icon name="chevron-down"/></sl:slot>
          </sl:dropdown-menu-trigger>
          <sl:dropdown-menu-content>x</sl:dropdown-menu-content>
        </sl:dropdown-menu>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("button.dropdown-menu-trigger")
        .hasClass("btn")
        .hasAttribute("popovertarget", "sort");
    assertThat(rendered.select(".dropdown-menu-trigger > .btn-icon[data-icon='inline-start'] > svg")).hasSize(1);
    assertThat(rendered.select(".dropdown-menu-trigger > .btn-icon[data-icon='inline-end'] > svg")).hasSize(1);
  }

  @Test
  void aButtonWithoutIconSlotsGetsNoIconWrappers() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu id="m">
          <sl:dropdown-menu-trigger>Open</sl:dropdown-menu-trigger>
        </sl:dropdown-menu>""");

    assertThat(rendered.select(".btn-icon")).isEmpty();
    assertThat(rendered).element(".dropdown-menu-trigger").hasText("Open");
  }

  @Test
  void aSidebarMenuButtonOpensTheMenu() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu id="teams">
          <sl:dropdown-menu-trigger as="sidebar-menu-button" size="lg" tooltip="Acme Inc." data-test="x">
            <sl:slot name="icon-start"><sl:icon name="gallery-vertical-end"/></sl:slot>
            <strong>Acme Inc.</strong>
            <small>Enterprise</small>
            <sl:slot name="icon-end"><sl:icon name="chevrons-up-down"/></sl:slot>
          </sl:dropdown-menu-trigger>
          <sl:dropdown-menu-content side="right">x</sl:dropdown-menu-content>
        </sl:dropdown-menu>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("button.sidebar-menu-button")
        .hasClass("dropdown-menu-trigger")
        .hasAttribute("type", "button")
        .hasAttribute("id", "teams-trigger")
        .hasAttribute("popovertarget", "teams")
        .hasAttribute("aria-haspopup", "menu")
        .hasAttribute("data-size", "lg")
        .hasAttribute("data-test", "x")
        .hasNoAttribute("data-variant", "aria-current", "aria-expanded");
    // icon-start, label, icon-end, then the tooltip
    assertThat(rendered.select(".sidebar-menu-button > *").eachAttr("class")).containsExactly(
        "sl-icon", "sidebar-menu-button-label", "sl-icon", "tooltip-content sidebar-menu-tooltip");
    assertThat(rendered).element(".sidebar-menu-button-label > strong").hasText("Acme Inc.");
    assertThat(rendered).element(".sidebar-menu-tooltip")
        .hasText("Acme Inc.")
        .hasAttribute("aria-hidden", "true");
    assertThat(rendered).element(".dropdown-menu-content").hasAttribute("aria-labelledby", "teams-trigger");
  }

  @Test
  void aSidebarMenuButtonWithoutIconsOrTooltip() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu id="v">
          <sl:dropdown-menu-trigger as="sidebar-menu-button" variant="outline" size="sm">v1.0</sl:dropdown-menu-trigger>
        </sl:dropdown-menu>""");

    assertThat(rendered.select(".sidebar-menu-button > *").eachAttr("class"))
        .containsExactly("sidebar-menu-button-label");
    assertThat(rendered).element(".sidebar-menu-button")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("data-size", "sm");
  }

  @Test
  void aSidebarMenuButtonInACollapsibleItemIsStillAButton() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item collapsible>
          <sl:dropdown-menu id="m">
            <sl:dropdown-menu-trigger as="sidebar-menu-button">Open</sl:dropdown-menu-trigger>
          </sl:dropdown-menu>
        </sl:sidebar-menu-item>""");

    assertThat(rendered.select("summary")).isEmpty();
    assertThat(rendered).element("button.sidebar-menu-button").hasAttribute("popovertarget", "m");
  }

  @Test
  void aSidebarMenuButtonTakesOnlyItsOwnSizes() {
    assertThatRenderFailure(() -> tester.render("""
        <sl:dropdown-menu id="m">
          <sl:dropdown-menu-trigger as="sidebar-menu-button" size="icon">x</sl:dropdown-menu-trigger>
        </sl:dropdown-menu>"""))
        .hasMessageContaining("sidebar-menu-button")
        .hasMessageContaining("size");
  }

  @Test
  void aSidebarMenuActionOpensTheMenu() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu th:id="|project-${id}-more|">
          <sl:dropdown-menu-trigger as="sidebar-menu-action" show-on-hover aria-label="More for Design">
            <sl:icon name="ellipsis"/>
          </sl:dropdown-menu-trigger>
          <sl:dropdown-menu-content side="right">x</sl:dropdown-menu-content>
        </sl:dropdown-menu>""", Map.of("id", 3));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("button.sidebar-menu-action")
        .hasClass("dropdown-menu-trigger")
        .hasAttribute("type", "button")
        .hasAttribute("id", "project-3-more-trigger")
        .hasAttribute("popovertarget", "project-3-more")
        .hasAttribute("aria-haspopup", "menu")
        .hasAttribute("aria-label", "More for Design")
        .hasAttribute("data-show-on-hover", "true");
  }

  @Test
  void aSidebarMenuActionWithoutShowOnHover() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu id="m">
          <sl:dropdown-menu-trigger as="sidebar-menu-action" aria-label="More">...</sl:dropdown-menu-trigger>
        </sl:dropdown-menu>""");

    assertThat(rendered).element(".sidebar-menu-action").hasNoAttribute("data-show-on-hover");
  }

  @Test
  void aSidebarMenuActionNeedsAnAccessibleName() {
    assertThatRenderFailure(() -> tester.render("""
        <sl:dropdown-menu id="m">
          <sl:dropdown-menu-trigger as="sidebar-menu-action">...</sl:dropdown-menu-trigger>
        </sl:dropdown-menu>"""))
        .hasMessageContaining("needs an accessible name");
  }

  @Test
  void aSidebarMenuActionTriggerInAnItemBesideItsButton() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button href="/projects/design">Design</sl:sidebar-menu-button>
          <sl:dropdown-menu id="design-more">
            <sl:dropdown-menu-trigger as="sidebar-menu-action" aria-label="More for Design">...</sl:dropdown-menu-trigger>
            <sl:dropdown-menu-content side="right">
              <sl:dropdown-menu-item as="a" href="/projects/design">View project</sl:dropdown-menu-item>
            </sl:dropdown-menu-content>
          </sl:dropdown-menu>
        </sl:sidebar-menu-item>""");

    assertThat(rendered.select("li.sidebar-menu-item > *").eachAttr("class")).containsExactly(
        "sidebar-menu-button", "sidebar-menu-action dropdown-menu-trigger", "dropdown-menu-content");
  }
}
