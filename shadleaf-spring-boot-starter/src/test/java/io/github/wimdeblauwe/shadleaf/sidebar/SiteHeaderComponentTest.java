package io.github.wimdeblauwe.shadleaf.sidebar;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.junit.jupiter.api.Test;

class SiteHeaderComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void theSiteHeaderIsAHeaderElement() {
    Rendered rendered = tester.render("<sl:site-header id=\"top\"><a href=\"/\">Acme</a></sl:site-header>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("header")
        .hasClassName("site-header")
        .hasAttribute("id", "top")
        .hasNoAttribute("role");
    assertThat(rendered).element("header > a").hasText("Acme");
    assertThat(rendered).elements(".site-header-end").isEmpty();
  }

  @Test
  void theEndSlotGoesLastInItsOwnWrapper() {
    Rendered rendered = tester.render("""
        <sl:site-header>
          <sl:slot name="end"><button type="button">Search</button></sl:slot>
          <a href="/">Acme</a>
        </sl:site-header>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("header > a + div.site-header-end:last-child > button").hasText("Search");
  }

  @Test
  void theHeaderLayoutKeepsOneNavigation() {
    Rendered rendered = tester.render("""
        <sl:sidebar-provider>
          <sl:site-header>
            <sl:sidebar-trigger aria-label="Menu"><sl:icon name="menu"/></sl:sidebar-trigger>
            <sl:sidebar placement="header">
              <sl:sidebar-header panel-only><a href="/">Acme</a></sl:sidebar-header>
              <sl:sidebar-content>
                <sl:sidebar-group>
                  <sl:sidebar-menu>
                    <sl:sidebar-menu-item><sl:sidebar-menu-button href="/" active>Home</sl:sidebar-menu-button></sl:sidebar-menu-item>
                  </sl:sidebar-menu>
                </sl:sidebar-group>
                <sl:sidebar-group panel-only>
                  <sl:sidebar-menu>
                    <sl:sidebar-menu-item><sl:sidebar-menu-button href="/help">Help</sl:sidebar-menu-button></sl:sidebar-menu-item>
                  </sl:sidebar-menu>
                </sl:sidebar-group>
              </sl:sidebar-content>
            </sl:sidebar>
          </sl:site-header>
          <sl:sidebar-inset><h1>Home</h1></sl:sidebar-inset>
        </sl:sidebar-provider>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).elements("nav").hasSize(1);
    assertThat(rendered).element(".sidebar-provider > header.site-header > nav.sidebar[data-placement=header]")
        .hasAttribute("popover", "");
    assertThat(rendered).element(".site-header > .sidebar-trigger")
        .hasAttribute("popovertarget", "sidebar")
        .hasAttribute("aria-label", "Menu");
    assertThat(rendered).element(".sidebar-header").hasAttribute("data-panel-only", "true");
    assertThat(rendered).elements(".sidebar-group[data-panel-only=true]").hasSize(1);
    assertThat(rendered).element(".sidebar-provider > main.sidebar-inset").hasAttribute("id", "main");
  }
}
