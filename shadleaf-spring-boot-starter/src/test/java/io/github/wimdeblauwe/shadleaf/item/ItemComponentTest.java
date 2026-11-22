package io.github.wimdeblauwe.shadleaf.item;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.junit.jupiter.api.Test;

class ItemComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void rendersADivWithoutDataAttributesForTheDefaults() {
    Rendered rendered = tester.render("<sl:item>Content</sl:item>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClassName("item")
        .hasNoAttribute("data-variant", "data-size", "role");
  }

  @Test
  void rendersVariantAndSize() {
    assertThat(tester.render("<sl:item variant=\"muted\" size=\"xs\">Content</sl:item>")).root()
        .hasAttribute("data-variant", "muted")
        .hasAttribute("data-size", "xs");
  }

  @Test
  void canBeALink() {
    assertThat(tester.render("""
        <sl:item as="a" href="/dashboard" variant="outline" class="extra">Content</sl:item>""")).root()
        .hasTag("a")
        .hasClass("item", "extra")
        .hasAttribute("href", "/dashboard")
        .hasAttribute("data-variant", "outline")
        .hasNoAttribute("role");
  }

  @Test
  void groupIsAListOfListItems() {
    Rendered rendered = tester.render("""
        <sl:item-group>
          <sl:item>One</sl:item>
          <sl:item-separator/>
          <sl:item>Two</sl:item>
        </sl:item-group>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasClassName("item-group").hasAttribute("role", "list").hasNoAttribute("data-layout");
    assertThat(rendered).element(".item-group > .item:first-child").hasAttribute("role", "listitem");
    assertThat(rendered).element(".item-group > .item:last-child").hasAttribute("role", "listitem");
    assertThat(rendered).element(".item-group > .separator.item-separator").hasAttribute("role", "none");
  }

  @Test
  void linkInAGroupIsWrappedInAListItem() {
    // role="listitem" on the anchor would take away its link role.
    Rendered rendered = tester.render("""
        <sl:item-group><sl:item as="a" href="/settings" size="sm">Settings</sl:item></sl:item-group>""");

    assertThat(rendered).element(".item-group > div.item-listitem").hasAttribute("role", "listitem");
    assertThat(rendered).element(".item-group > .item-listitem > a.item")
        .hasAttribute("href", "/settings")
        .hasAttribute("data-size", "sm")
        .hasNoAttribute("role");
  }

  @Test
  void itemNestedInAListedItemIsNoListItem() {
    Rendered rendered = tester.render("""
        <sl:item-group><sl:item><sl:item-content><sl:item>Nested</sl:item></sl:item-content></sl:item></sl:item-group>""");

    assertThat(rendered).element(".item-group > .item").hasAttribute("role", "listitem");
    assertThat(rendered).element(".item-content > .item").hasNoAttribute("role");
  }

  @Test
  void itemAfterAGroupIsNoListItem() {
    Rendered rendered = tester.render("""
        <div><sl:item-group><sl:item>One</sl:item></sl:item-group><sl:item>After</sl:item></div>""");

    assertThat(rendered).element(".item-group + .item").hasNoAttribute("role");
  }

  @Test
  void gridGroup() {
    assertThat(tester.render("<sl:item-group layout=\"grid\">x</sl:item-group>")).root()
        .hasAttribute("data-layout", "grid")
        .hasAttribute("role", "list");
  }

  @Test
  void mediaVariants() {
    assertThat(tester.render("<sl:item-media>x</sl:item-media>")).root()
        .hasClassName("item-media").hasNoAttribute("data-variant");
    assertThat(tester.render("<sl:item-media variant=\"image\">x</sl:item-media>")).root()
        .hasAttribute("data-variant", "image");
  }

  @Test
  void titleIsAHeadingOnlyWithALevel() {
    assertThat(tester.render("<sl:item-title>Title</sl:item-title>")).root()
        .hasTag("div").hasClassName("item-title").hasNoAttribute("role", "aria-level");
    assertThat(tester.render("<sl:item-title level=\"3\">Title</sl:item-title>")).root()
        .hasAttribute("role", "heading")
        .hasAttribute("aria-level", "3");
  }

  @Test
  void descriptionIsAParagraph() {
    assertThat(tester.render("<sl:item-description>Text</sl:item-description>")).root()
        .hasTag("p").hasClassName("item-description").hasText("Text");
  }

  @Test
  void composes() {
    Rendered rendered = tester.render("""
        <sl:item variant="outline">
          <sl:item-header>Header</sl:item-header>
          <sl:item-media variant="icon"><sl:icon name="inbox"/></sl:item-media>
          <sl:item-content>
            <sl:item-title>Title</sl:item-title>
            <sl:item-description>Description</sl:item-description>
          </sl:item-content>
          <sl:item-actions><sl:button size="sm">Action</sl:button></sl:item-actions>
          <sl:item-footer>Footer</sl:item-footer>
        </sl:item>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element(".item > .item-header").hasText("Header");
    assertThat(rendered).element(".item > .item-media > svg").hasAttribute("aria-hidden", "true");
    assertThat(rendered).element(".item > .item-content > .item-title").hasText("Title");
    assertThat(rendered).element(".item > .item-actions > button.button").hasText("Action");
    assertThat(rendered).element(".item > .item-footer").hasText("Footer");
  }
}
