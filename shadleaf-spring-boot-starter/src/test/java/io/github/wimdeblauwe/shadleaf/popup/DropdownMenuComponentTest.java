package io.github.wimdeblauwe.shadleaf.popup;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class DropdownMenuComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  private static final String MENU = """
      <sl:dropdown-menu id="account">
        <sl:dropdown-menu-trigger variant="outline">Open</sl:dropdown-menu-trigger>
        <sl:dropdown-menu-content>
          <sl:dropdown-menu-item>Profile</sl:dropdown-menu-item>
        </sl:dropdown-menu-content>
      </sl:dropdown-menu>""";

  @Test
  void triggerOpensTheMenuWithPopovertarget() {
    Rendered rendered = tester.render(MENU);

    assertThat(rendered).hasNoLeakedMarkup();
    // The root renders no element of its own: the trigger and the menu are siblings.
    assertThat(rendered.document().body().children()).hasSize(2);
    assertThat(rendered).element("button.dropdown-menu-trigger")
        .hasClass("btn")
        .hasAttribute("id", "account-trigger")
        .hasAttribute("popovertarget", "account")
        .hasAttribute("aria-haspopup", "menu")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("type", "button")
        // Alpine sets it, so the browser's own expanded state stands without Alpine
        .hasNoAttribute("aria-expanded");
    assertThat(rendered).element("div.dropdown-menu-content")
        .hasAttribute("id", "account")
        .hasAttribute("role", "menu")
        .hasAttribute("popover", "")
        .hasAttribute("tabindex", "-1")
        .hasAttribute("x-data", "slDropdownMenu")
        .hasAttribute("aria-labelledby", "account-trigger")
        .hasNoAttribute("data-side", "data-align");
  }

  @Test
  void needsAnId() {
    assertThatRenderFailure(() -> tester.render("<sl:dropdown-menu>x</sl:dropdown-menu>"))
        .hasMessageContaining("<sl:dropdown-menu> needs id");
  }

  @Test
  void idFromAnExpressionReachesTheParts() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu th:id="|member-${id}-actions|">
          <sl:dropdown-menu-trigger size="icon-sm" aria-label="Actions">...</sl:dropdown-menu-trigger>
          <sl:dropdown-menu-content>x</sl:dropdown-menu-content>
        </sl:dropdown-menu>""", Map.of("id", 7));

    assertThat(rendered).element(".dropdown-menu-trigger")
        .hasAttribute("id", "member-7-actions-trigger")
        .hasAttribute("popovertarget", "member-7-actions")
        .hasAttribute("aria-label", "Actions");
    assertThat(rendered).element(".dropdown-menu-content")
        .hasAttribute("id", "member-7-actions")
        .hasAttribute("aria-labelledby", "member-7-actions-trigger");
  }

  @Test
  void iconTriggerNeedsAnAccessibleName() {
    assertThatRenderFailure(() -> tester.render("""
        <sl:dropdown-menu id="m"><sl:dropdown-menu-trigger size="icon">...</sl:dropdown-menu-trigger></sl:dropdown-menu>"""))
        .hasMessageContaining("needs an accessible name");
  }

  @Test
  void aMenuWithoutTheTriggerPartIsNotLabelledByIt() {
    Rendered rendered = tester.render("""
        <sl:button popovertarget="m">Elsewhere</sl:button>
        <sl:dropdown-menu id="m">
          <sl:dropdown-menu-content aria-label="Row actions">x</sl:dropdown-menu-content>
        </sl:dropdown-menu>""");

    assertThat(rendered).element(".dropdown-menu-content")
        .hasNoAttribute("aria-labelledby")
        .hasAttribute("aria-label", "Row actions");
  }

  @Test
  void sideAndAlignAreDataAttributesUnlessDefault() {
    Element content = tester.render("""
        <sl:dropdown-menu id="m"><sl:dropdown-menu-content side="top" align="end">x</sl:dropdown-menu-content>
        </sl:dropdown-menu>""").root();

    assertThat(content).hasAttribute("data-side", "top").hasAttribute("data-align", "end");
  }

  @Test
  void nestedMenusKeepTheirOwnIds() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu id="outer">
          <sl:dropdown-menu-trigger>Outer</sl:dropdown-menu-trigger>
          <sl:dropdown-menu-content>
            <sl:dropdown-menu id="inner">
              <sl:dropdown-menu-trigger>Inner</sl:dropdown-menu-trigger>
              <sl:dropdown-menu-content>x</sl:dropdown-menu-content>
            </sl:dropdown-menu>
          </sl:dropdown-menu-content>
        </sl:dropdown-menu>""");

    assertThat(rendered).element("#outer").hasAttribute("aria-labelledby", "outer-trigger");
    assertThat(rendered).element("#inner").hasAttribute("aria-labelledby", "inner-trigger");
    assertThat(rendered).element("#inner-trigger").hasAttribute("popovertarget", "inner");
  }

  @Test
  void itemIsAButtonThatPassesItsAttributesThrough() {
    Element item = tester.render("""
        <sl:dropdown-menu-item class="extra" hx-delete="/members/1" hx-target="closest tr">Delete
        </sl:dropdown-menu-item>""").root();

    assertThat(item)
        .hasTag("button")
        .hasAttribute("class", "dropdown-menu-item extra")
        .hasAttribute("type", "button")
        .hasAttribute("role", "menuitem")
        .hasAttribute("hx-delete", "/members/1")
        .hasAttribute("hx-target", "closest tr")
        // slDropdownMenu takes the items out of the tab order; without Alpine, Tab reaches them
        .hasNoAttribute("tabindex", "data-variant", "aria-checked", "disabled");
  }

  @Test
  void itemAsALink() {
    Element item = tester.render("""
        <sl:dropdown-menu-item as="a" th:href="@{/settings}">Settings</sl:dropdown-menu-item>""").root();

    assertThat(item).hasTag("a").hasAttribute("href", "/settings").hasAttribute("role", "menuitem")
        .hasNoAttribute("type");
  }

  @Test
  void disabledItems() {
    assertThat(tester.render("<sl:dropdown-menu-item disabled>Archive</sl:dropdown-menu-item>")).root()
        .hasAttribute("disabled");
    assertThat(tester.render("""
        <sl:dropdown-menu-item as="a" href="/a" th:href="@{/b}" disabled>Link</sl:dropdown-menu-item>""")).root()
        .hasAttribute("aria-disabled", "true")
        .hasNoAttribute("href", "disabled");
  }

  @Test
  void destructiveAndInset() {
    assertThat(tester.render("""
        <sl:dropdown-menu-item variant="destructive" inset>Delete</sl:dropdown-menu-item>""")).root()
        .hasAttribute("data-variant", "destructive")
        .hasAttribute("data-inset", "true");
  }

  @Test
  void submitItem() {
    assertThat(tester.render("<sl:dropdown-menu-item type=\"submit\" form=\"logout\">Log out</sl:dropdown-menu-item>"))
        .root()
        .hasAttribute("type", "submit")
        .hasAttribute("form", "logout");
  }

  @Test
  void checkboxAndRadioItemsShowTheStateTheServerGives() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu-checkbox-item th:checked="${archived}" as="a" href="?archived=false">Archived
        </sl:dropdown-menu-checkbox-item>
        <sl:dropdown-menu-radio-group aria-label="Sort by">
          <sl:dropdown-menu-radio-item th:checked="${sort == 'name'}">Name</sl:dropdown-menu-radio-item>
          <sl:dropdown-menu-radio-item th:checked="${sort == 'date'}">Date</sl:dropdown-menu-radio-item>
        </sl:dropdown-menu-radio-group>""", Map.of("archived", true, "sort", "date"));

    assertThat(rendered).element("a.dropdown-menu-checkbox-item")
        .hasAttribute("role", "menuitemcheckbox")
        .hasAttribute("aria-checked", "true")
        .hasAttribute("href", "?archived=false")
        .element(".dropdown-menu-item-indicator svg");
    assertThat(rendered).element(".dropdown-menu-radio-group")
        .hasAttribute("role", "group")
        .hasAttribute("aria-label", "Sort by");
    assertThat(rendered).elements("button.dropdown-menu-radio-item[role=menuitemradio]")
        .extracting(element -> element.attr("aria-checked"))
        .containsExactly("false", "true");
  }

  @Test
  void labelGroupSeparatorAndShortcut() {
    Rendered rendered = tester.render("""
        <sl:dropdown-menu-label inset>My account</sl:dropdown-menu-label>
        <sl:dropdown-menu-group aria-label="Account">
          <sl:dropdown-menu-item>Profile <sl:dropdown-menu-shortcut>⇧⌘P</sl:dropdown-menu-shortcut>
          </sl:dropdown-menu-item>
        </sl:dropdown-menu-group>
        <sl:dropdown-menu-separator/>""");

    assertThat(rendered).element("div.dropdown-menu-label").hasAttribute("data-inset", "true")
        .hasNoAttribute("role");
    assertThat(rendered).element(".dropdown-menu-group").hasAttribute("role", "group");
    assertThat(rendered).element(".dropdown-menu-item > span.dropdown-menu-shortcut").hasText("⇧⌘P");
    assertThat(rendered).element(".dropdown-menu-separator").hasAttribute("role", "separator");
  }
}
