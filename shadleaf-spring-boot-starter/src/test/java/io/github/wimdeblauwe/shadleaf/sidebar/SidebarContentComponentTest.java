package io.github.wimdeblauwe.shadleaf.sidebar;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The sidebar's content parts: header, content, footer, separator, groups and menus. */
class SidebarContentComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void headerContentAndFooterArePlainDivs() {
    Rendered rendered = tester.render("""
        <sl:sidebar>
          <sl:sidebar-header>Acme</sl:sidebar-header>
          <sl:sidebar-separator/>
          <sl:sidebar-content>Links</sl:sidebar-content>
          <sl:sidebar-footer>Help</sl:sidebar-footer>
        </sl:sidebar>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("nav.sidebar > div.sidebar-header").hasText("Acme");
    assertThat(rendered).element("nav.sidebar > div.sidebar-content").hasText("Links").hasNoAttribute("role");
    assertThat(rendered).element("nav.sidebar > div.sidebar-footer").hasText("Help");
    // One navigation landmark: the content is no second nav.
    assertThat(rendered).elements("nav").hasSize(1);
    assertThat(rendered).element(".sidebar-separator").hasTag("div").hasAttribute("role", "none");
  }

  @Test
  void aGroupWithALabelIsAGroupNamedByIt() {
    Rendered rendered = tester.render("""
        <sl:sidebar-group id="platform">
          <sl:sidebar-group-label>Platform</sl:sidebar-group-label>
          <sl:sidebar-group-content>
            <sl:sidebar-menu><sl:sidebar-menu-item>x</sl:sidebar-menu-item></sl:sidebar-menu>
          </sl:sidebar-group-content>
        </sl:sidebar-group>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("div")
        .hasClassName("sidebar-group")
        .hasAttribute("id", "platform")
        .hasAttribute("role", "group")
        .hasAttribute("aria-labelledby", "platform-label");
    assertThat(rendered).element(".sidebar-group-label").hasTag("div").hasAttribute("id", "platform-label")
        .hasNoAttribute("role");
    assertThat(rendered).element(".sidebar-group-content > ul.sidebar-menu > li.sidebar-menu-item").hasText("x");
  }

  @Test
  void aGroupWithoutAnIdGeneratesOneForItsLabel() {
    Rendered rendered = tester.render("""
        <sl:sidebar-group><sl:sidebar-group-label>Platform</sl:sidebar-group-label></sl:sidebar-group>""");

    Element group = rendered.root();
    assertThat(group).hasNoAttribute("id");
    assertThat(group.attr("aria-labelledby")).matches("sl-sidebar-group-[a-z0-9]{8}-label");
    assertThat(rendered).element(".sidebar-group-label").hasAttribute("id", group.attr("aria-labelledby"));
  }

  @Test
  void aGroupWithoutALabelIsAPlainDiv() {
    Rendered rendered = tester.render("""
        <sl:sidebar-group><sl:sidebar-group-content>x</sl:sidebar-group-content></sl:sidebar-group>""");

    assertThat(rendered).root().hasNoAttribute("role", "aria-labelledby", "id");
  }

  @Test
  void aNestedGroupHasItsOwnLabel() {
    Rendered rendered = tester.render("""
        <sl:sidebar-group id="outer">
          <sl:sidebar-group-label>Outer</sl:sidebar-group-label>
          <sl:sidebar-group id="inner"><sl:sidebar-group-content>x</sl:sidebar-group-content></sl:sidebar-group>
          <sl:sidebar-group id="named"><sl:sidebar-group-label>Named</sl:sidebar-group-label></sl:sidebar-group>
        </sl:sidebar-group>""");

    assertThat(rendered).root().hasAttribute("aria-labelledby", "outer-label");
    assertThat(rendered).element("#inner").hasNoAttribute("role", "aria-labelledby");
    assertThat(rendered).element("#named").hasAttribute("aria-labelledby", "named-label");
    assertThat(rendered).element("#named > .sidebar-group-label").hasAttribute("id", "named-label");
  }

  @Test
  void aLabelOutsideAGroupHasNoId() {
    assertThat(tester.render("<sl:sidebar-group-label>Platform</sl:sidebar-group-label>")).root()
        .hasNoAttribute("id");
  }

  @Test
  void theMenuButtonIsALinkByDefault() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-button href="/inbox">
          <sl:slot name="icon-start"><sl:icon name="inbox"/></sl:slot>
          Inbox
        </sl:sidebar-menu-button>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("a")
        .hasClassName("sidebar-menu-button")
        .hasAttribute("href", "/inbox")
        .hasNoAttribute("aria-current", "aria-describedby", "data-size", "data-variant", "type", "role");
    assertThat(rendered).element("a > svg:first-child").hasAttribute("aria-hidden", "true");
    assertThat(rendered).element("a > span.sidebar-menu-button-label").hasText("Inbox");
  }

  @Test
  void anActiveLinkIsTheCurrentPage() {
    assertThat(tester.render("<sl:sidebar-menu-button href=\"/\" active>Home</sl:sidebar-menu-button>")).root()
        .hasAttribute("aria-current", "page");
  }

  @Test
  void anActiveButtonIsTheCurrentItem() {
    assertThat(tester.render("<sl:sidebar-menu-button as=\"button\" active>Drafts</sl:sidebar-menu-button>")).root()
        .hasTag("button")
        .hasAttribute("type", "button")
        .hasAttribute("aria-current", "true");
  }

  @Test
  void aDisabledLinkLosesItsHref() {
    assertThat(tester.render("<sl:sidebar-menu-button href=\"/billing\" disabled>Billing</sl:sidebar-menu-button>"))
        .root()
        .hasNoAttribute("href")
        .hasAttribute("aria-disabled", "true")
        .hasAttribute("role", "link")
        .hasAttribute("tabindex", "-1");
    assertThat(tester.render("<sl:sidebar-menu-button th:href=\"@{/billing}\" disabled>Billing</sl:sidebar-menu-button>"))
        .root().hasNoAttribute("href");
  }

  @Test
  void aDisabledButtonIsDisabled() {
    assertThat(tester.render("<sl:sidebar-menu-button as=\"button\" disabled>Archive</sl:sidebar-menu-button>")).root()
        .hasAttribute("disabled");
  }

  @Test
  void aBadgeDescribesTheButtonOfItsItem() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item id="inbox">
          <sl:sidebar-menu-button href="/inbox">Inbox</sl:sidebar-menu-button>
          <sl:sidebar-menu-badge>12</sl:sidebar-menu-badge>
        </sl:sidebar-menu-item>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("li").hasAttribute("id", "inbox");
    assertThat(rendered).element(".sidebar-menu-button").hasAttribute("aria-describedby", "inbox-badge");
    assertThat(rendered).element(".sidebar-menu-badge").hasTag("div").hasAttribute("id", "inbox-badge")
        .hasText("12");
  }

  @Test
  void anItemWithoutAnIdGeneratesOneForItsBadge() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item>
          <sl:sidebar-menu-button href="/inbox">Inbox</sl:sidebar-menu-button>
          <sl:sidebar-menu-badge>12</sl:sidebar-menu-badge>
        </sl:sidebar-menu-item>""");

    String id = rendered.select(".sidebar-menu-badge").first().id();
    assertThat(id).matches("sl-sidebar-menu-item-[a-z0-9]{8}-badge");
    assertThat(rendered).root().hasNoAttribute("id");
    assertThat(rendered).element(".sidebar-menu-button").hasAttribute("aria-describedby", id);
  }

  @Test
  void anItemWithoutABadgeDescribesNothing() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item id="home">
          <sl:sidebar-menu-button href="/">Home</sl:sidebar-menu-button>
          <sl:sidebar-menu>
            <sl:sidebar-menu-item>
              <sl:sidebar-menu-button href="/x">Nested</sl:sidebar-menu-button>
              <sl:sidebar-menu-badge>3</sl:sidebar-menu-badge>
            </sl:sidebar-menu-item>
          </sl:sidebar-menu>
        </sl:sidebar-menu-item>""");

    assertThat(rendered).element("li#home > .sidebar-menu-button").hasNoAttribute("aria-describedby");
    assertThat(rendered).element("li li > .sidebar-menu-button").hasAttribute("aria-describedby");
  }

  @Test
  void aBadgeLabelReplacesWhatAScreenReaderHears() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-badge label="12 unread messages">12</sl:sidebar-menu-badge>""");

    assertThat(rendered).element(".sidebar-menu-badge > span[aria-hidden=true]").hasText("12");
    assertThat(rendered).element(".sidebar-menu-badge > span.sl-sr-only").hasText("12 unread messages");
  }

  @Test
  void anOwnAriaDescribedbyReplacesTheBadge() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-item id="inbox">
          <sl:sidebar-menu-button href="/inbox" aria-describedby="hint">Inbox</sl:sidebar-menu-button>
          <sl:sidebar-menu-badge>12</sl:sidebar-menu-badge>
        </sl:sidebar-menu-item>""");

    assertThat(rendered).element(".sidebar-menu-button").hasAttribute("aria-describedby", "hint");
  }

  @Test
  void theMenuActionIsAButtonBesideTheItem() {
    Rendered rendered = tester.render("""
        <sl:sidebar-menu-action aria-label="More for Design" show-on-hover><sl:icon name="ellipsis"/>\
        </sl:sidebar-menu-action>""");

    assertThat(rendered).root()
        .hasTag("button")
        .hasClassName("sidebar-menu-action")
        .hasAttribute("type", "button")
        .hasAttribute("aria-label", "More for Design")
        .hasAttribute("data-show-on-hover", "true");
    assertThat(tester.render("<sl:sidebar-menu-action as=\"a\" href=\"/x\" aria-label=\"Open\">x</sl:sidebar-menu-action>"))
        .root().hasTag("a").hasAttribute("href", "/x").hasNoAttribute("type", "data-show-on-hover");
  }

  @Test
  void theGroupActionIsAButtonBesideTheLabel() {
    assertThat(tester.render("""
        <sl:sidebar-group-action aria-label="Add project"><sl:icon name="plus"/></sl:sidebar-group-action>"""))
        .root()
        .hasTag("button")
        .hasClassName("sidebar-group-action")
        .hasAttribute("type", "button")
        .hasAttribute("aria-label", "Add project");
  }

  @ParameterizedTest
  @ValueSource(strings = {"sidebar-group-action", "sidebar-menu-action"})
  void anActionNeedsAnAccessibleName(String part) {
    assertThatRenderFailure(() -> tester.render("<sl:%s><sl:icon name=\"plus\"/></sl:%s>".formatted(part, part)))
        .hasMessageContaining("<sl:" + part + ">")
        .hasMessageContaining("accessible name");
    assertThat(tester.render("<sl:%s th:aria-label=\"${'Add'}\">x</sl:%s>".formatted(part, part))).root()
        .hasAttribute("aria-label", "Add");
  }

  @Test
  void panelOnlyPartsAreMarked() {
    for (String part : new String[]{"sidebar-header", "sidebar-footer", "sidebar-group", "sidebar-menu-item"}) {
      assertThat(tester.render("<sl:%s panel-only>x</sl:%s>".formatted(part, part))).root()
          .hasClassName(part)
          .hasAttribute("data-panel-only", "true");
      assertThat(tester.render("<sl:%s>x</sl:%s>".formatted(part, part))).root()
          .hasNoAttribute("data-panel-only");
    }
  }
}
