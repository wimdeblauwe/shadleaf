package io.github.wimdeblauwe.shadleaf.dialog;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SheetComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void isANativeDialogOnTheRightThatClosesOnAClickOutside() {
    Rendered rendered = tester.render("<sl:sheet id=\"filters\">Content</sl:sheet>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("dialog")
        .hasClassName("sheet")
        .hasAttribute("id", "filters")
        .hasAttribute("closedby", "any")
        .hasAttribute("x-data", "slDialog")
        .hasNoAttribute("role")
        .hasNoAttribute("open")
        .hasNoAttribute("data-side")
        .hasNoAttribute("data-show-modal");
  }

  @Test
  void needsAnId() {
    assertThatRenderFailure(() -> tester.render("<sl:sheet>Content</sl:sheet>"))
        .hasMessageContaining("<sl:sheet> needs id");
  }

  @ParameterizedTest
  @ValueSource(strings = {"top", "bottom", "left"})
  void side(String side) {
    assertThat(tester.render("<sl:sheet id=\"filters\" side=\"%s\">Content</sl:sheet>".formatted(side))).root()
        .hasAttribute("data-side", side);
  }

  @Test
  void rightIsTheDefault() {
    assertThat(tester.render("<sl:sheet id=\"filters\" side=\"right\">Content</sl:sheet>")).root()
        .hasNoAttribute("data-side");
  }

  @Test
  void titleAndDescriptionNameAndDescribeTheSheet() {
    Element sheet = tester.render("""
        <sl:sheet id="filters">
          <sl:sheet-header>
            <sl:sheet-title>Filters</sl:sheet-title>
            <sl:sheet-description>Narrow the list down.</sl:sheet-description>
          </sl:sheet-header>
        </sl:sheet>""").root();

    assertThat(sheet)
        .hasAttribute("aria-labelledby", "filters-title")
        .hasAttribute("aria-describedby", "filters-description");
    assertThat(sheet.selectFirst(".sheet-title[role=heading][aria-level=2]")).hasAttribute("id", "filters-title");
    assertThat(sheet.selectFirst(".sheet-description")).hasAttribute("id", "filters-description");
  }

  @Test
  void aDialogInASheetKeepsItsOwnTitle() {
    Element sheet = tester.render("""
        <sl:sheet id="filters">
          <sl:dialog id="help"><sl:dialog-title>Help</sl:dialog-title></sl:dialog>
        </sl:sheet>""").root();

    assertThat(sheet).hasNoAttribute("aria-labelledby");
    assertThat(sheet.selectFirst("#help .dialog-title")).hasAttribute("id", "help-title");
  }

  @Test
  void closeButtonClosesThisSheet() {
    Element close = tester.render("<sl:sheet id=\"filters\">Content</sl:sheet>").root()
        .selectFirst("button.sheet-close");

    assertThat(close)
        .hasAttribute("commandfor", "filters")
        .hasAttribute("command", "close")
        .hasAttribute("aria-label", "Close")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon-sm");
  }

  @Test
  void closeButtonCanBeLeftOut() {
    assertThat(tester.render("<sl:sheet id=\"filters\" close-button=\"false\">Content</sl:sheet>").root()
        .selectFirst("button")).isNull();
  }

  @Test
  void openShowsItAsAModalOnceAlpineStarts() {
    assertThat(tester.render("<sl:sheet id=\"filters\" open>Content</sl:sheet>")).root()
        .hasAttribute("data-show-modal", "true")
        .hasNoAttribute("open");
  }

  @Test
  void footerCloseButton() {
    Element close = tester.render("""
        <sl:sheet id="filters" close-button="false">
          <sl:sheet-footer close-button><sl:button type="submit">Apply</sl:button></sl:sheet-footer>
        </sl:sheet>""").root().select(".sheet-footer > button").last();

    assertThat(close)
        .hasText("Close")
        .hasAttribute("commandfor", "filters")
        .hasAttribute("command", "close")
        .hasAttribute("data-variant", "outline");
  }
}
