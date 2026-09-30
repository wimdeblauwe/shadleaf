package io.github.wimdeblauwe.shadleaf.dialog;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class DialogComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void isANativeDialogThatClosesOnAClickOutside() {
    Rendered rendered = tester.render("<sl:dialog id=\"edit\">Content</sl:dialog>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("dialog")
        .hasClassName("dialog")
        .hasAttribute("id", "edit")
        .hasAttribute("closedby", "any")
        .hasAttribute("x-data", "slDialog")
        .hasNoAttribute("open")
        .hasNoAttribute("aria-labelledby")
        .hasNoAttribute("aria-describedby")
        .hasNoAttribute("data-show-modal");
  }

  @Test
  void needsAnId() {
    assertThatRenderFailure(() -> tester.render("<sl:dialog>Content</sl:dialog>"))
        .hasMessageContaining("<sl:dialog> needs id");
  }

  @Test
  void titleAndDescriptionNameAndDescribeTheDialog() {
    Element dialog = tester.render("""
        <sl:dialog id="edit">
          <sl:dialog-header>
            <sl:dialog-title>Edit profile</sl:dialog-title>
            <sl:dialog-description>Make changes.</sl:dialog-description>
          </sl:dialog-header>
        </sl:dialog>""").root();

    assertThat(dialog).hasAttribute("aria-labelledby", "edit-title").hasAttribute("aria-describedby",
        "edit-description");
    assertThat(dialog.selectFirst(".dialog-title[role=heading][aria-level=2]")).hasAttribute("id", "edit-title");
    assertThat(dialog.selectFirst(".dialog-description")).hasAttribute("id", "edit-description");
  }

  @Test
  void idFromAnExpressionReachesTheParts() {
    Element dialog = tester.render("""
        <sl:dialog th:id="|delete-${order}|">
          <sl:dialog-header><sl:dialog-title>Delete</sl:dialog-title></sl:dialog-header>
        </sl:dialog>""", Map.of("order", 42)).root();

    assertThat(dialog).hasAttribute("id", "delete-42").hasAttribute("aria-labelledby", "delete-42-title");
    assertThat(dialog.selectFirst(".dialog-title")).hasAttribute("id", "delete-42-title");
  }

  @Test
  void ownAriaLabelWins() {
    Element dialog = tester.render("""
        <sl:dialog id="edit" aria-labelledby="elsewhere">
          <sl:dialog-title>Edit</sl:dialog-title>
        </sl:dialog>""").root();

    assertThat(dialog).hasAttribute("aria-labelledby", "elsewhere");
  }

  @Test
  void nestedDialogsKeepTheirOwnTitles() {
    Element outer = tester.render("""
        <sl:dialog id="outer">
          <sl:dialog id="inner"><sl:dialog-title>Inner</sl:dialog-title></sl:dialog>
        </sl:dialog>""").root();

    assertThat(outer).hasNoAttribute("aria-labelledby");
    assertThat(outer.selectFirst("#inner")).hasAttribute("aria-labelledby", "inner-title");
    assertThat(outer.selectFirst("#inner .dialog-title")).hasAttribute("id", "inner-title");
  }

  @Test
  void closeButtonClosesThisDialog() {
    Element close = tester.render("<sl:dialog id=\"edit\">Content</sl:dialog>").root()
        .selectFirst("button.dialog-close");

    assertThat(close)
        .hasAttribute("commandfor", "edit")
        .hasAttribute("command", "close")
        .hasAttribute("aria-label", "Close")
        .hasAttribute("type", "button")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon-sm");
    assertThat(close.selectFirst("svg")).isNotNull();
  }

  @Test
  void closeButtonCanBeLeftOut() {
    Element dialog = tester.render("<sl:dialog id=\"edit\" close-button=\"false\">Content</sl:dialog>").root();

    assertThat(dialog.selectFirst("button")).isNull();
  }

  @Test
  void openShowsItAsAModalOnceAlpineStarts() {
    assertThat(tester.render("<sl:dialog id=\"edit\" open>Content</sl:dialog>")).root()
        .hasAttribute("data-show-modal", "true")
        // never the open attribute: that shows a dialog, but not as a modal
        .hasNoAttribute("open");
  }

  @Test
  void footerCloseButton() {
    Element dialog = tester.render("""
        <sl:dialog id="edit" close-button="false">
          <sl:dialog-footer close-button><sl:button type="submit">Save</sl:button></sl:dialog-footer>
        </sl:dialog>""").root();

    Element close = dialog.select(".dialog-footer > button").last();
    assertThat(close)
        .hasText("Close")
        .hasAttribute("commandfor", "edit")
        .hasAttribute("command", "close")
        .hasAttribute("data-variant", "outline");
  }
}
