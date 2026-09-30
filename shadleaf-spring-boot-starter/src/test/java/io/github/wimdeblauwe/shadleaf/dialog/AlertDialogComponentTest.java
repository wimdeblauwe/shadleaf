package io.github.wimdeblauwe.shadleaf.dialog;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Locale;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class AlertDialogComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void isANativeAlertDialogThatAClickOutsideDoesNotClose() {
    Rendered rendered = tester.render("<sl:alert-dialog id=\"confirm\">Content</sl:alert-dialog>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("dialog")
        .hasClassName("alert-dialog")
        .hasAttribute("id", "confirm")
        .hasAttribute("role", "alertdialog")
        .hasAttribute("closedby", "closerequest")
        .hasAttribute("x-data", "slDialog")
        .hasNoAttribute("open")
        .hasNoAttribute("data-size")
        .hasNoAttribute("data-show-modal");
    // no close button in the corner: the user answers the question
    assertThat(rendered.root().selectFirst("button")).isNull();
  }

  @Test
  void needsAnId() {
    assertThatRenderFailure(() -> tester.render("<sl:alert-dialog>Content</sl:alert-dialog>"))
        .hasMessageContaining("<sl:alert-dialog> needs id");
  }

  @Test
  void smallSize() {
    assertThat(tester.render("<sl:alert-dialog id=\"confirm\" size=\"sm\">Content</sl:alert-dialog>")).root()
        .hasAttribute("data-size", "sm");
  }

  @Test
  void openShowsItAsAModalOnceAlpineStarts() {
    assertThat(tester.render("<sl:alert-dialog id=\"confirm\" open>Content</sl:alert-dialog>")).root()
        .hasAttribute("data-show-modal", "true")
        .hasNoAttribute("open");
  }

  @Test
  void titleAndDescriptionNameAndDescribeTheDialog() {
    Element dialog = tester.render("""
        <sl:alert-dialog th:id="|delete-${order}|">
          <sl:alert-dialog-header>
            <sl:alert-dialog-media><sl:icon name="trash-2"/></sl:alert-dialog-media>
            <sl:alert-dialog-title>Delete order?</sl:alert-dialog-title>
            <sl:alert-dialog-description>This cannot be undone.</sl:alert-dialog-description>
          </sl:alert-dialog-header>
        </sl:alert-dialog>""", Map.of("order", 42)).root();

    assertThat(dialog)
        .hasAttribute("id", "delete-42")
        .hasAttribute("aria-labelledby", "delete-42-title")
        .hasAttribute("aria-describedby", "delete-42-description");
    assertThat(dialog.selectFirst(".alert-dialog-title[role=heading][aria-level=2]"))
        .hasAttribute("id", "delete-42-title");
    assertThat(dialog.selectFirst(".alert-dialog-description")).hasAttribute("id", "delete-42-description");
    assertThat(dialog.selectFirst(".alert-dialog-media > svg")).hasAttribute("aria-hidden", "true");
  }

  @Test
  void aNestedAlertDialogKeepsItsOwnTitle() {
    Element outer = tester.render("""
        <sl:dialog id="outer">
          <sl:dialog-header><sl:dialog-title>Outer</sl:dialog-title></sl:dialog-header>
          <sl:alert-dialog id="inner"><sl:alert-dialog-title>Inner</sl:alert-dialog-title></sl:alert-dialog>
        </sl:dialog>""").root();

    assertThat(outer).hasAttribute("aria-labelledby", "outer-title");
    assertThat(outer.selectFirst(".dialog-title")).hasAttribute("id", "outer-title");
    assertThat(outer.selectFirst("#inner")).hasAttribute("aria-labelledby", "inner-title");
    assertThat(outer.selectFirst("#inner .alert-dialog-title")).hasAttribute("id", "inner-title");
  }

  @Test
  void cancelClosesThisDialogAndTakesTheFocus() {
    Element cancel = tester.render("""
        <sl:alert-dialog id="confirm">
          <sl:alert-dialog-footer><sl:alert-dialog-cancel/><sl:button>Continue</sl:button></sl:alert-dialog-footer>
        </sl:alert-dialog>""").root().selectFirst(".alert-dialog-footer > button");

    assertThat(cancel)
        .hasText("Cancel")
        .hasClass("btn", "alert-dialog-cancel")
        .hasAttribute("type", "button")
        .hasAttribute("data-variant", "outline")
        .hasNoAttribute("data-size")
        .hasAttribute("commandfor", "confirm")
        .hasAttribute("command", "close")
        .hasAttribute("autofocus", "");
  }

  @Test
  void cancelPassesItsPropsAndAttributesToTheButton() {
    Element cancel = tester.render("""
        <sl:alert-dialog id="confirm">
          <sl:alert-dialog-cancel variant="ghost" size="sm" class="wide" data-test="x"
                                  th:aria-label="|Keep order ${order}|">Keep it</sl:alert-dialog-cancel>
        </sl:alert-dialog>""", Map.of("order", 42)).root().selectFirst("button");

    assertThat(cancel)
        .hasText("Keep it")
        .hasAttribute("class", "btn alert-dialog-cancel wide")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "sm")
        .hasAttribute("data-test", "x")
        .hasAttribute("aria-label", "Keep order 42")
        .hasAttribute("commandfor", "confirm");
  }

  @Test
  void cancelTextComesFromTheMessages() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.alert-dialog.cancel", Locale.ENGLISH, "No, keep it");
    ShadleafMessageSource.attachTo(messages);
    ComponentRenderTester reworded = ComponentRenderTester.builder().messageSource(messages).build();

    assertThat(reworded.render("<sl:alert-dialog-cancel/>").root()).hasText("No, keep it");
  }

  @Test
  void destructiveMedia() {
    assertThat(tester.render("<sl:alert-dialog-media variant=\"destructive\">x</sl:alert-dialog-media>")).root()
        .hasClassName("alert-dialog-media")
        .hasAttribute("data-variant", "destructive");
  }
}
