package io.github.wimdeblauwe.shadleaf.button;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.StaticMessageSource;

class ButtonComponentTest {

  private static final List<String> VARIANTS = List.of("primary", "secondary", "outline", "ghost", "link",
      "destructive");
  private static final List<String> SIZES = List.of("xs", "sm", "default", "lg", "icon", "icon-xs", "icon-sm",
      "icon-lg");

  private final ComponentRenderer renderer = new ComponentRenderer();

  @Test
  void plainButtonIsABareBtn() {
    String html = renderer.render("<sl:button>Save</sl:button>");
    Element button = first(html);

    assertThat(button.tagName()).isEqualTo("button");
    assertThat(button.attributes().asList()).extracting(Attribute::getKey).containsExactly("class", "type");
    assertThat(button.className()).isEqualTo("btn");
    assertThat(button.attr("type")).isEqualTo("button");
    assertThat(button.text()).isEqualTo("Save");
    assertThat(html).doesNotContain("<!--");
  }

  @Test
  void withoutContentShowsTheFallbackLabel() {
    assertThat(render("<sl:button/>").text()).isEqualTo("Button");
  }

  static List<Arguments> matrix() {
    List<Arguments> arguments = new ArrayList<>();
    for (String variant : VARIANTS) {
      for (String size : SIZES) {
        arguments.add(Arguments.of(variant, size));
      }
    }
    return arguments;
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("matrix")
  void everyVariantAndSizeRendersAsDataAttributesExceptTheDefaults(String variant, String size) {
    Element button = render("<sl:button variant=\"%s\" size=\"%s\" aria-label=\"Add\">Add</sl:button>"
        .formatted(variant, size));

    assertThat(button.attr("data-variant")).isEqualTo(variant.equals("primary") ? "" : variant);
    assertThat(button.hasAttr("data-variant")).isEqualTo(!variant.equals("primary"));
    assertThat(button.attr("data-size")).isEqualTo(size.equals("default") ? "" : size);
    assertThat(button.hasAttr("data-size")).isEqualTo(!size.equals("default"));
    assertThat(button.attributes().asList()).extracting(Attribute::getKey)
        .doesNotContain("variant", "size", "as", "loading");
  }

  @Test
  void variantCanBeAnExpression() {
    Element button = render("<sl:button th:variant=\"${admin ? 'destructive' : 'primary'}\">Delete</sl:button>",
        Map.of("admin", true));

    assertThat(button.attr("data-variant")).isEqualTo("destructive");
  }

  @Test
  void illegalVariantFailsListingTheLegalOnes() {
    assertThatThrownBy(() -> render("<sl:button variant=\"destructve\">Delete</sl:button>"))
        .rootCause()
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessageContaining("destructve")
        .hasMessageContaining("primary, secondary, outline, ghost, link, destructive");
  }

  @Test
  void typeSubmitAndReset() {
    assertThat(render("<sl:button type=\"submit\">Save</sl:button>").attr("type")).isEqualTo("submit");
    assertThat(render("<sl:button type=\"reset\">Reset</sl:button>").attr("type")).isEqualTo("reset");
  }

  @Test
  void passesThroughHtmxAlpineAriaAndDataAttributesAndMergesClass() {
    Element button = render("""
        <sl:button variant="destructive" size="sm" class="ml-auto" hx-delete="/orders/42"
                   hx-confirm="Delete this order?" x-on:click="open = false" aria-describedby="help"
                   data-order="42" id="delete">Delete order</sl:button>""");

    assertThat(button.className()).isEqualTo("btn ml-auto");
    assertThat(button.attr("hx-delete")).isEqualTo("/orders/42");
    assertThat(button.attr("hx-confirm")).isEqualTo("Delete this order?");
    assertThat(button.attr("x-on:click")).isEqualTo("open = false");
    assertThat(button.attr("aria-describedby")).isEqualTo("help");
    assertThat(button.attr("data-order")).isEqualTo("42");
    assertThat(button.id()).isEqualTo("delete");
  }

  @Test
  void passedThroughThAttributesAreEvaluated() {
    Element button = render("<sl:button th:text=\"${label}\" th:hx-post=\"@{orders/{id}(id=${id})}\">x</sl:button>",
        Map.of("label", "Sync", "id", 7));

    assertThat(button.text()).isEqualTo("Sync");
    assertThat(button.attr("hx-post")).isEqualTo("orders/7");
  }

  @Test
  void disabledButton() {
    Element button = render("<sl:button disabled>Save</sl:button>");

    assertThat(button.hasAttr("disabled")).isTrue();
    assertThat(button.hasAttr("aria-busy")).isFalse();
  }

  @Test
  void disabledFalseIsEnabled() {
    assertThat(render("<sl:button th:disabled=\"${false}\">Save</sl:button>").hasAttr("disabled")).isFalse();
  }

  // --- as="a" -----------------------------------------------------------------------------------

  @Test
  void anchorLooksTheSameAndKeepsItsHref() {
    Element anchor = render("<sl:button as=\"a\" variant=\"outline\" href=\"/orders\">Orders</sl:button>");

    assertThat(anchor.tagName()).isEqualTo("a");
    assertThat(anchor.className()).isEqualTo("btn");
    assertThat(anchor.attr("data-variant")).isEqualTo("outline");
    assertThat(anchor.attr("href")).isEqualTo("/orders");
    assertThat(anchor.hasAttr("type")).isFalse();
    assertThat(anchor.hasAttr("aria-disabled")).isFalse();
    assertThat(anchor.hasAttr("tabindex")).isFalse();
    assertThat(anchor.hasAttr("role")).isFalse();
    assertThat(anchor.text()).isEqualTo("Orders");
  }

  // A relative link: the render harness has no web context, which @{/...} needs.
  @Test
  void anchorHrefCanBeAnExpression() {
    assertThat(render("<sl:button as=\"a\" th:href=\"@{orders/{id}(id=${id})}\">Order</sl:button>",
        Map.of("id", 7)).attr("href")).isEqualTo("orders/7");
  }

  @Test
  void disabledAnchorCannotBeFollowedOrFocused() {
    Element literal = render("<sl:button as=\"a\" disabled href=\"/orders\">Orders</sl:button>");
    Element expression = render("<sl:button as=\"a\" disabled th:href=\"@{/orders}\">Orders</sl:button>");

    for (Element anchor : List.of(literal, expression)) {
      assertThat(anchor.hasAttr("href")).isFalse();
      assertThat(anchor.attr("aria-disabled")).isEqualTo("true");
      assertThat(anchor.attr("tabindex")).isEqualTo("-1");
      assertThat(anchor.attr("role")).isEqualTo("link");
      assertThat(anchor.hasAttr("disabled")).isFalse();
    }
  }

  @Test
  void loadingAnchorIsDisabledToo() {
    Element anchor = render("<sl:button as=\"a\" loading href=\"/orders\">Orders</sl:button>");

    assertThat(anchor.hasAttr("href")).isFalse();
    assertThat(anchor.attr("aria-disabled")).isEqualTo("true");
    assertThat(anchor.attr("aria-busy")).isEqualTo("true");
    assertThat(anchor.selectFirst(".btn-spinner")).isNotNull();
  }

  // --- loading ----------------------------------------------------------------------------------

  @Test
  void loadingShowsASpinnerAndAHiddenLabelAndDisables() {
    Element button = render("<sl:button loading>Save</sl:button>");

    assertThat(button.attr("aria-busy")).isEqualTo("true");
    assertThat(button.hasAttr("disabled")).isTrue();
    Element spinner = button.selectFirst("> .btn-icon[data-icon=inline-start] > svg.sl-icon.btn-spinner");
    assertThat(spinner).isNotNull();
    assertThat(spinner.attr("aria-hidden")).isEqualTo("true");
    assertThat(button.selectFirst("> .sl-sr-only").text()).isEqualTo("Loading");
    assertThat(button.text()).as("the hidden label is read first").isEqualTo("Loading Save");
  }

  @Test
  void spinnerTakesThePlaceOfTheStartIcon() {
    Element button = render("""
        <sl:button loading>
          <sl:slot name="icon-start"><sl:icon name="save"/></sl:slot>
          Save
        </sl:button>""");

    assertThat(button.select("> .btn-icon")).hasSize(1);
    assertThat(button.select("svg")).hasSize(1);
    assertThat(button.selectFirst("svg").hasClass("btn-spinner")).isTrue();
  }

  @Test
  void loadingIconOnlyButtonShowsOnlyTheSpinner() {
    Element button = render("""
        <sl:button size="icon" loading aria-label="Delete"><sl:icon name="trash"/></sl:button>""");

    assertThat(button.select("svg")).hasSize(1);
    assertThat(button.selectFirst("svg").hasClass("btn-spinner")).isTrue();
    assertThat(button.attr("aria-label")).isEqualTo("Delete");
  }

  @Test
  void loadingLabelComesFromTheApplicationsMessages() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.button.loading", Locale.forLanguageTag("nl"), "Bezig");
    ShadleafMessageSource.attachTo(messages);
    ComponentRenderer dutch = new ComponentRenderer();
    dutch.engine().setTemplateEngineMessageSource(messages);

    String html = dutch.engine().process("<sl:button loading>Opslaan</sl:button>",
        new org.thymeleaf.context.Context(Locale.forLanguageTag("nl")));

    assertThat(first(html).selectFirst(".sl-sr-only").text()).isEqualTo("Bezig");
  }

  // --- icon slots -------------------------------------------------------------------------------

  @Test
  void iconNestedInTheIconStartSlot() {
    Element button = render("""
        <sl:button variant="destructive">
          <sl:slot name="icon-start"><sl:icon name="trash"/></sl:slot>
          Delete order
        </sl:button>""");

    Element wrapper = button.selectFirst("> span.btn-icon");
    assertThat(wrapper.attr("data-icon")).isEqualTo("inline-start");
    Element icon = wrapper.selectFirst("> svg.sl-icon");
    assertThat(icon).isNotNull();
    assertThat(icon.attr("aria-hidden")).isEqualTo("true");
    assertThat(icon.select("path")).hasSize(5);
    assertThat(button.select("> span.btn-icon")).hasSize(1);
    assertThat(button.text()).isEqualTo("Delete order");
    assertThat(button.attr("data-variant")).as("the icon's props do not leak into the button")
        .isEqualTo("destructive");
  }

  @Test
  void iconEndSlot() {
    Element button = render("""
        <sl:button>Next<sl:slot name="icon-end"><sl:icon name="arrow-right"/></sl:slot></sl:button>""");

    assertThat(button.selectFirst("> span.btn-icon").attr("data-icon")).isEqualTo("inline-end");
    assertThat(button.child(button.childrenSize() - 1).hasClass("btn-icon")).isTrue();
  }

  @Test
  void noIconWrapperWithoutIcons() {
    assertThat(render("<sl:button>Save</sl:button>").select(".btn-icon")).isEmpty();
  }

  @Test
  void explicitlySizedIconIsMarkedSoTheButtonLeavesItsSizeAlone() {
    Element icon = render("""
        <sl:button><sl:slot name="icon-start"><sl:icon name="plus" size="20"/></sl:slot>Add</sl:button>""")
        .selectFirst("svg");

    assertThat(icon.attr("data-icon-size")).isEqualTo("20");
  }

  // --- accessible name --------------------------------------------------------------------------

  @ParameterizedTest
  @MethodSource("iconSizes")
  void iconOnlyButtonWithoutAriaLabelFails(String size) {
    assertThatThrownBy(() -> render("<sl:button size=\"%s\"><sl:icon name=\"trash\"/></sl:button>".formatted(size)))
        .rootCause()
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessageContaining("<sl:button size=\"%s\">".formatted(size))
        .hasMessageContaining("aria-label");
  }

  static List<String> iconSizes() {
    return SIZES.stream().filter(size -> size.startsWith("icon")).toList();
  }

  @Test
  void iconOnlyButtonWithAnAccessibleName() {
    Element labelled = render("<sl:button size=\"icon\" aria-label=\"Delete\"><sl:icon name=\"trash\"/></sl:button>");
    Element expression = render("<sl:button size=\"icon\" th:aria-label=\"${label}\"><sl:icon name=\"trash\"/></sl:button>",
        Map.of("label", "Delete"));
    Element labelledBy = render("<sl:button size=\"icon\" aria-labelledby=\"t\"><sl:icon name=\"trash\"/></sl:button>");

    assertThat(labelled.attr("aria-label")).isEqualTo("Delete");
    assertThat(labelled.attr("data-size")).isEqualTo("icon");
    assertThat(expression.attr("aria-label")).isEqualTo("Delete");
    assertThat(labelledBy.attr("aria-labelledby")).isEqualTo("t");
  }

  @Test
  void textButtonNeedsNoAriaLabel() {
    assertThat(render("<sl:button size=\"sm\">Save</sl:button>").hasAttr("aria-label")).isFalse();
  }

  private Element render(String snippet) {
    return render(snippet, Map.of());
  }

  private Element render(String snippet, Map<String, ?> variables) {
    return first(renderer.render(snippet, variables));
  }

  private static Element first(String html) {
    return Jsoup.parseBodyFragment(html).body().child(0);
  }
}
