package io.github.wimdeblauwe.shadleaf.button;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void plainButtonIsABareBtn() {
    Rendered rendered = tester.render("<sl:button>Save</sl:button>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("button")
        .hasAttributeNames("class", "type")
        .hasClassName("btn")
        .hasAttribute("type", "button")
        .hasText("Save");
  }

  @Test
  void withoutContentShowsTheFallbackLabel() {
    assertThat(tester.render("<sl:button/>")).root().hasText("Button");
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
    var button = assertThat(tester.render(
        "<sl:button variant=\"%s\" size=\"%s\" aria-label=\"Add\">Add</sl:button>".formatted(variant, size))).root()
        .hasNoAttribute("variant", "size", "as", "loading");

    if (variant.equals("primary")) {
      button.hasNoAttribute("data-variant");
    } else {
      button.hasAttribute("data-variant", variant);
    }
    if (size.equals("default")) {
      button.hasNoAttribute("data-size");
    } else {
      button.hasAttribute("data-size", size);
    }
  }

  @Test
  void variantCanBeAnExpression() {
    assertThat(tester.render("<sl:button th:variant=\"${admin ? 'destructive' : 'primary'}\">Delete</sl:button>",
        Map.of("admin", true))).root()
        .hasAttribute("data-variant", "destructive");
  }

  @Test
  void illegalVariantFailsListingTheLegalOnes() {
    assertThatRenderFailure(() -> tester.render("<sl:button variant=\"destructve\">Delete</sl:button>"))
        .hasMessageContaining("destructve")
        .hasMessageContaining("primary, secondary, outline, ghost, link, destructive");
  }

  @Test
  void typeSubmitAndReset() {
    assertThat(tester.render("<sl:button type=\"submit\">Save</sl:button>")).root().hasAttribute("type", "submit");
    assertThat(tester.render("<sl:button type=\"reset\">Reset</sl:button>")).root().hasAttribute("type", "reset");
  }

  @Test
  void passesThroughHtmxAlpineAriaAndDataAttributesAndMergesClass() {
    assertThat(tester.render("""
        <sl:button variant="destructive" size="sm" class="ml-auto" hx-delete="/orders/42"
                   hx-confirm="Delete this order?" x-on:click="open = false" aria-describedby="help"
                   data-order="42" id="delete">Delete order</sl:button>""")).root()
        .hasClassName("btn ml-auto")
        .hasAttribute("hx-delete", "/orders/42")
        .hasAttribute("hx-confirm", "Delete this order?")
        .hasAttribute("x-on:click", "open = false")
        .hasAttribute("aria-describedby", "help")
        .hasAttribute("data-order", "42")
        .hasAttribute("id", "delete");
  }

  @Test
  void passedThroughThAttributesAreEvaluated() {
    assertThat(tester.render("<sl:button th:text=\"${label}\" th:hx-post=\"@{/orders/{id}(id=${id})}\">x</sl:button>",
        Map.of("label", "Sync", "id", 7))).root()
        .hasText("Sync")
        .hasAttribute("hx-post", "/orders/7");
  }

  @Test
  void disabledButton() {
    assertThat(tester.render("<sl:button disabled>Save</sl:button>")).root()
        .hasAttribute("disabled")
        .hasNoAttribute("aria-busy");
  }

  @Test
  void disabledFalseIsEnabled() {
    assertThat(tester.render("<sl:button th:disabled=\"${false}\">Save</sl:button>")).root()
        .hasNoAttribute("disabled");
  }

  // --- as="a" -----------------------------------------------------------------------------------

  @Test
  void anchorLooksTheSameAndKeepsItsHref() {
    assertThat(tester.render("<sl:button as=\"a\" variant=\"outline\" href=\"/orders\">Orders</sl:button>")).root()
        .hasTag("a")
        .hasClassName("btn")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("href", "/orders")
        .hasNoAttribute("type", "aria-disabled", "tabindex", "role")
        .hasText("Orders");
  }

  @Test
  void anchorHrefCanBeAnExpressionAndHonoursTheContextPath() {
    ComponentRenderTester shop = ComponentRenderTester.builder().contextPath("/shop").build();

    assertThat(shop.render("<sl:button as=\"a\" th:href=\"@{/orders/{id}(id=${id})}\">Order</sl:button>",
        Map.of("id", 7))).root()
        .hasAttribute("href", "/shop/orders/7");
  }

  @Test
  void disabledAnchorCannotBeFollowedOrFocused() {
    for (String snippet : List.of("<sl:button as=\"a\" disabled href=\"/orders\">Orders</sl:button>",
        "<sl:button as=\"a\" disabled th:href=\"@{/orders}\">Orders</sl:button>")) {
      assertThat(tester.render(snippet)).root()
          .hasNoAttribute("href", "disabled")
          .hasAttribute("aria-disabled", "true")
          .hasAttribute("tabindex", "-1")
          .hasAttribute("role", "link");
    }
  }

  @Test
  void loadingAnchorIsDisabledToo() {
    assertThat(tester.render("<sl:button as=\"a\" loading href=\"/orders\">Orders</sl:button>")).root()
        .hasNoAttribute("href")
        .hasAttribute("aria-disabled", "true")
        .hasAttribute("aria-busy", "true")
        .element(".btn-spinner");
  }

  // --- loading ----------------------------------------------------------------------------------

  @Test
  void loadingShowsASpinnerAndAHiddenLabelAndDisables() {
    var button = assertThat(tester.render("<sl:button loading>Save</sl:button>")).root()
        .hasAttribute("aria-busy", "true")
        .hasAttribute("disabled")
        .as("the hidden label is read first").hasText("Loading Save");
    button.element("> .btn-icon[data-icon=inline-start] > svg.sl-icon.btn-spinner").hasAttribute("aria-hidden", "true");
    button.element("> .sl-sr-only").hasText("Loading");
  }

  @Test
  void spinnerTakesThePlaceOfTheStartIcon() {
    Rendered rendered = tester.render("""
        <sl:button loading>
          <sl:slot name="icon-start"><sl:icon name="save"/></sl:slot>
          Save
        </sl:button>""");

    assertThat(rendered).elements(".btn > .btn-icon").hasSize(1);
    assertThat(rendered).elements("svg").singleElement().satisfies(svg -> assertThat(svg).hasClass("btn-spinner"));
  }

  @Test
  void loadingIconOnlyButtonShowsOnlyTheSpinner() {
    Rendered rendered = tester.render("""
        <sl:button size="icon" loading aria-label="Delete"><sl:icon name="trash"/></sl:button>""");

    assertThat(rendered).elements("svg").singleElement().satisfies(svg -> assertThat(svg).hasClass("btn-spinner"));
    assertThat(rendered).root().hasAttribute("aria-label", "Delete");
  }

  @Test
  void loadingLabelComesFromTheApplicationsMessages() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.button.loading", Locale.forLanguageTag("nl"), "Bezig");
    ShadleafMessageSource.attachTo(messages);
    ComponentRenderTester dutch = ComponentRenderTester.builder()
        .messageSource(messages)
        .locale(Locale.forLanguageTag("nl"))
        .build();

    assertThat(dutch.render("<sl:button loading>Opslaan</sl:button>")).element(".sl-sr-only").hasText("Bezig");
  }

  // --- icon slots -------------------------------------------------------------------------------

  @Test
  void iconNestedInTheIconStartSlot() {
    Rendered rendered = tester.render("""
        <sl:button variant="destructive">
          <sl:slot name="icon-start"><sl:icon name="trash"/></sl:slot>
          Delete order
        </sl:button>""");

    assertThat(rendered).elements(".btn > span.btn-icon").hasSize(1);
    assertThat(rendered).elements("svg path").hasSize(5);
    var button = assertThat(rendered).root()
        .hasText("Delete order")
        .as("the icon's props do not leak into the button").hasAttribute("data-variant", "destructive");
    button.element("> span.btn-icon")
        .hasAttribute("data-icon", "inline-start")
        .element("> svg.sl-icon").hasAttribute("aria-hidden", "true");
  }

  @Test
  void iconEndSlot() {
    Rendered rendered = tester.render("""
        <sl:button>Next<sl:slot name="icon-end"><sl:icon name="arrow-right"/></sl:slot></sl:button>""");

    assertThat(rendered).element(".btn > span.btn-icon").hasAttribute("data-icon", "inline-end");
    assertThat(rendered).element(".btn > :last-child").hasClass("btn-icon");
  }

  @Test
  void noIconWrapperWithoutIcons() {
    assertThat(tester.render("<sl:button>Save</sl:button>")).hasNoElement(".btn-icon");
  }

  @Test
  void explicitlySizedIconIsMarkedSoTheButtonLeavesItsSizeAlone() {
    assertThat(tester.render("""
        <sl:button><sl:slot name="icon-start"><sl:icon name="plus" size="20"/></sl:slot>Add</sl:button>"""))
        .element("svg").hasAttribute("data-icon-size", "20");
  }

  // --- accessible name --------------------------------------------------------------------------

  @ParameterizedTest
  @MethodSource("iconSizes")
  void iconOnlyButtonWithoutAriaLabelFails(String size) {
    assertThatRenderFailure(
        () -> tester.render("<sl:button size=\"%s\"><sl:icon name=\"trash\"/></sl:button>".formatted(size)))
        .hasMessageContaining("<sl:button size=\"%s\">".formatted(size))
        .hasMessageContaining("aria-label");
  }

  static List<String> iconSizes() {
    return SIZES.stream().filter(size -> size.startsWith("icon")).toList();
  }

  @Test
  void iconOnlyButtonWithAnAccessibleName() {
    assertThat(tester.render("<sl:button size=\"icon\" aria-label=\"Delete\"><sl:icon name=\"trash\"/></sl:button>"))
        .root().hasAttribute("aria-label", "Delete").hasAttribute("data-size", "icon");
    assertThat(tester.render("<sl:button size=\"icon\" th:aria-label=\"${label}\"><sl:icon name=\"trash\"/></sl:button>",
        Map.of("label", "Delete"))).root().hasAttribute("aria-label", "Delete");
    assertThat(tester.render("<sl:button size=\"icon\" aria-labelledby=\"t\"><sl:icon name=\"trash\"/></sl:button>"))
        .root().hasAttribute("aria-labelledby", "t");
  }

  @Test
  void textButtonNeedsNoAriaLabel() {
    assertThat(tester.render("<sl:button size=\"sm\">Save</sl:button>")).root().hasNoAttribute("aria-label");
  }
}
