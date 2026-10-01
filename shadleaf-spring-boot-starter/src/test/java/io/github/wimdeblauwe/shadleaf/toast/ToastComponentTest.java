package io.github.wimdeblauwe.shadleaf.toast;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class ToastComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void theToasterIsARegionWithAnnouncersAndATemplatePerVariant() {
    Rendered rendered = tester.render("<sl:toaster/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("div")
        .hasClassName("toaster")
        .hasAttribute("id", "toaster")
        .hasAttribute("x-data", "slToaster")
        .hasNoAttribute("data-duration", "data-visible-toasts", "data-position", "popover");
    assertThat(rendered).element(".toaster > .toaster-viewport")
        .hasAttribute("popover", "manual")
        .hasAttribute("role", "region")
        .hasAttribute("aria-label", "Notifications (Alt+T)")
        .hasNoAttribute("data-position", "aria-live");
    assertThat(rendered).element(".toaster-viewport > ol.toaster-list").hasText("");
    assertThat(rendered).elements(".toaster-viewport > .toaster-announcer")
        .extracting(element -> element.attr("role"), element -> element.attr("aria-live"), Element::text)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("status", "polite", ""),
            org.assertj.core.groups.Tuple.tuple("alert", "assertive", ""));
    assertThat(rendered).elements(".toaster > template.toaster-template")
        .extracting(element -> element.attr("data-variant"))
        .containsExactly("default", "success", "info", "warning", "error");
  }

  @Test
  void positionDurationVisibleToastsAndOwnAttributes() {
    Rendered rendered = tester.render(
        "<sl:toaster id=\"notes\" position=\"top-center\" duration=\"8000\" visible-toasts=\"5\" class=\"app\"/>");

    assertThat(rendered).root()
        .hasAttribute("id", "notes")
        .hasAttribute("data-duration", "8000")
        .hasAttribute("data-visible-toasts", "5")
        .hasAttribute("class", "toaster app");
    assertThat(rendered).element(".toaster-viewport").hasAttribute("data-position", "top-center");
  }

  @Test
  void aToastHasATitleADescriptionAndACloseButton() {
    Rendered rendered = tester.render(
        "<sl:toast title=\"Event created\" description=\"Monday, January 3rd at 6:00pm\"/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("li")
        .hasClassName("toast")
        .hasAttribute("tabindex", "-1")
        .hasNoAttribute("data-variant", "data-duration", "role", "aria-live")
        .hasNoElement("> svg");
    assertThat(rendered).element(".toast-content > .toast-title").hasText("Event created");
    assertThat(rendered).element(".toast-content > .toast-description").hasText("Monday, January 3rd at 6:00pm");
    assertThat(rendered).element(".toast > button.btn.toast-close")
        .hasAttribute("type", "button")
        .hasAttribute("aria-label", "Close")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon-xs")
        .element("svg");
  }

  @Test
  void eachVariantHasItsIcon() {
    Map<String, String> icons = Map.of("success", "circle-check", "info", "info", "warning", "triangle-alert",
        "error", "octagon-x");
    icons.forEach((variant, icon) -> {
      Rendered rendered = tester.render("<sl:toast variant=\"%s\" title=\"x\"/>".formatted(variant));
      assertThat(rendered).root().hasAttribute("data-variant", variant).hasNoElement(".toast-description");
      Element svg = rendered.root().child(0);
      assertThat(svg).hasTag("svg").hasAttribute("aria-hidden", "true");
      Rendered expected = tester.render("<sl:icon name=\"%s\"/>".formatted(icon));
      assertThat(svg.html()).as(variant).isEqualTo(expected.root().html());
    });
  }

  @Test
  void ownContentReplacesTheDescriptionAndAnIconSlotTheVariantsIcon() {
    Rendered rendered = tester.render("""
        <sl:toast variant="success" title="Invitation sent" duration="0">
          <sl:slot name="icon"><sl:icon name="mail"/></sl:slot>
          Sent to <b>ada@example.com</b>.
        </sl:toast>""");

    assertThat(rendered).root().hasAttribute("data-duration", "0");
    assertThat(rendered.root().select("> svg")).hasSize(1);
    assertThat(rendered).element(".toast-description").hasText("Sent to ada@example.com.").element("b");
  }

  @Test
  void theToasterRendersTheToastsInItsList() {
    Rendered rendered = tester.render("""
        <sl:toaster>
          <sl:toast th:each="toast : ${toasts}" th:title="${toast.title}" th:description="${toast.description}"
                    th:variant="${toast.variant}" th:duration="${toast.duration}"/>
        </sl:toaster>""", Map.of("toasts", List.of(
        Toast.success("Message sent").withDescription("We answer within a day."),
        Toast.error("Could not reach the mail server").untilClosed())));

    assertThat(rendered).elements(".toaster-list > li.toast")
        .extracting(element -> element.attr("data-variant"), element -> element.select(".toast-title").text(),
            element -> element.select(".toast-description").text(), element -> element.attr("data-duration"))
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("success", "Message sent", "We answer within a day.", ""),
            org.assertj.core.groups.Tuple.tuple("error", "Could not reach the mail server", "", "0"));
  }

  @Test
  void theTemplatesHoldAToastWithAnEmptyTitleAndDescription() {
    Rendered rendered = tester.render("<sl:toaster/>");

    Element template = rendered.root().selectFirst("template[data-variant=error]");
    Element toast = template.selectFirst("li.toast");
    assertThat(toast).hasAttribute("data-variant", "error");
    assertThat(toast).element(".toast-title").hasText("");
    assertThat(toast).element(".toast-description").hasText("");
    assertThat(rendered.root().selectFirst("template[data-variant=default] li.toast"))
        .hasNoAttribute("data-variant")
        .hasNoElement("> svg");
  }
}
