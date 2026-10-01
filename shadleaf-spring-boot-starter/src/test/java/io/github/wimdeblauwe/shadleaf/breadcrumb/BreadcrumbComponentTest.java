package io.github.wimdeblauwe.shadleaf.breadcrumb;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BreadcrumbComponentTest {

  private static final String TRAIL = """
      <sl:breadcrumb>
        <sl:breadcrumb-list>
          <sl:breadcrumb-item><sl:breadcrumb-link href="/">Home</sl:breadcrumb-link></sl:breadcrumb-item>
          <sl:breadcrumb-separator/>
          <sl:breadcrumb-item>Acme</sl:breadcrumb-item>
          <sl:breadcrumb-separator/>
          <sl:breadcrumb-item><sl:breadcrumb-page>People</sl:breadcrumb-page></sl:breadcrumb-item>
        </sl:breadcrumb-list>
      </sl:breadcrumb>""";

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void aTrailIsANavWithAnOrderedList() {
    Rendered rendered = tester.render(TRAIL);

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("nav")
        .hasClassName("breadcrumb")
        .hasAttribute("aria-label", "Breadcrumb");
    assertThat(rendered).element("nav > ol").hasClassName("breadcrumb-list");
    assertThat(rendered).elements("ol > li").hasSize(5);
    assertThat(rendered).element("li.breadcrumb-item > a")
        .hasClassName("breadcrumb-link")
        .hasAttribute("href", "/")
        .hasText("Home");
    assertThat(rendered).element("li.breadcrumb-item:nth-child(3)").hasText("Acme");
    assertThat(rendered).element("li.breadcrumb-item > span")
        .hasClassName("breadcrumb-page")
        .hasAttribute("aria-current", "page")
        .hasNoAttribute("role", "aria-disabled")
        .hasText("People");
  }

  @Test
  void anAriaLabelReplacesTheName() {
    assertThat(tester.render("<sl:breadcrumb aria-label=\"Folders\">x</sl:breadcrumb>")).root()
        .hasAttribute("aria-label", "Folders");
    assertThat(tester.render("<sl:breadcrumb th:aria-label=\"${name}\">x</sl:breadcrumb>", Map.of("name", "Path")))
        .root().hasAttribute("aria-label", "Path");
  }

  @Test
  void theSeparatorIsADecorativeListItemWithAChevron() {
    Rendered rendered = tester.render("<sl:breadcrumb-separator/>");

    assertThat(rendered).root()
        .hasTag("li")
        .hasClassName("breadcrumb-separator")
        .hasAttribute("role", "presentation")
        .hasAttribute("aria-hidden", "true");
    assertThat(rendered).root().element("svg").hasClass("breadcrumb-separator-icon");
  }

  @Test
  void contentReplacesTheSeparatorsChevron() {
    Rendered rendered = tester.render("<sl:breadcrumb-separator>/</sl:breadcrumb-separator>");

    assertThat(rendered).root().hasText("/").hasNoElement("svg");
  }

  @Test
  void anEmptyEllipsisSaysMoreLevels() {
    Rendered rendered = tester.render("<sl:breadcrumb-ellipsis/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("span")
        .hasClassName("breadcrumb-ellipsis")
        .hasNoAttribute("aria-hidden", "id", "role");
    assertThat(rendered).root().element("svg").hasAttribute("aria-hidden", "true");
    assertThat(rendered).root().element(".sl-sr-only").hasText("More levels");
  }

  @Test
  void anEllipsisWithItemsOpensAMenuOfThem() {
    Rendered rendered = tester.render("""
        <sl:breadcrumb-ellipsis>
          <sl:dropdown-menu-item as="a" href="/docs">Documentation</sl:dropdown-menu-item>
          <sl:dropdown-menu-item as="a" href="/themes">Themes</sl:dropdown-menu-item>
        </sl:breadcrumb-ellipsis>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).hasNoElement(".breadcrumb-ellipsis");
    assertThat(rendered).element("button")
        .hasClass("btn", "dropdown-menu-trigger", "breadcrumb-ellipsis-trigger")
        .hasAttribute("id", "breadcrumb-more-trigger")
        .hasAttribute("popovertarget", "breadcrumb-more")
        .hasAttribute("aria-haspopup", "menu")
        .hasAttribute("aria-label", "Show more levels")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon-sm");
    assertThat(rendered).element("#breadcrumb-more")
        .hasClassName("dropdown-menu-content")
        .hasAttribute("role", "menu")
        .hasAttribute("aria-labelledby", "breadcrumb-more-trigger")
        .hasNoAttribute("data-side", "data-align");
    assertThat(rendered).elements("#breadcrumb-more a[role=menuitem]").hasSize(2);
  }

  @Test
  void theMenusIdAndNameCanBeChanged() {
    Rendered rendered = tester.render("""
        <sl:breadcrumb-ellipsis id="path-more" aria-label="Folders above" data-test="x">
          <sl:dropdown-menu-item as="a" href="/docs">Documentation</sl:dropdown-menu-item>
        </sl:breadcrumb-ellipsis>""");

    assertThat(rendered).element("button")
        .hasAttribute("id", "path-more-trigger")
        .hasAttribute("popovertarget", "path-more")
        .hasAttribute("aria-label", "Folders above")
        .hasAttribute("data-test", "x");
    assertThat(rendered).element("#path-more").hasAttribute("role", "menu");
    assertThat(tester.render("""
        <sl:breadcrumb-ellipsis th:aria-label="${name}">
          <sl:dropdown-menu-item as="a" href="/docs">Documentation</sl:dropdown-menu-item>
        </sl:breadcrumb-ellipsis>""", Map.of("name", "Up"))).element("button").hasAttribute("aria-label", "Up");
  }

  @Test
  void theSkipLinkGoesToMain() {
    Rendered rendered = tester.render("<sl:skip-link/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("a")
        .hasClassName("skip-link")
        .hasAttribute("href", "#main")
        .hasText("Skip to main content");
  }

  @Test
  void theSkipLinksTargetAndTextCanBeChanged() {
    assertThat(tester.render("<sl:skip-link for=\"content\">Skip to the list</sl:skip-link>")).root()
        .hasAttribute("href", "#content")
        .hasText("Skip to the list");
    assertThat(tester.render("<sl:skip-link th:for=\"${target}\"/>", Map.of("target", "results"))).root()
        .hasAttribute("href", "#results");
  }

  @Test
  void theInsetIsTheSkipLinksDefaultTarget() {
    assertThat(tester.render("<sl:sidebar-inset>x</sl:sidebar-inset>")).root()
        .hasAttribute("id", "main")
        .hasAttribute("tabindex", "-1");
    assertThat(tester.render("<sl:sidebar-inset id=\"content\">x</sl:sidebar-inset>")).root()
        .hasAttribute("id", "content");
  }
}
