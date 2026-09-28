package io.github.wimdeblauwe.shadleaf.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class ComponentElementProcessorTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  // --- props -----------------------------------------------------------------------------------

  @Test
  void defaultsRenderWithoutDataAttributes() {
    Element chip = render("<sl:test-chip>New</sl:test-chip>");

    assertThat(chip.tagName()).isEqualTo("span");
    assertThat(chip.className()).isEqualTo("chip");
    assertThat(chip.attributes().asList()).extracting(a -> a.getKey()).containsExactly("class");
    assertThat(chip.text()).isEqualTo("New");
  }

  @Test
  void literalPropsAreBoundAndNotPassedThrough() {
    Element chip = render("<sl:test-chip variant=\"danger\" size=\"lg\" removable count=\"3\" label=\"Tag:\">x</sl:test-chip>");

    assertThat(chip.attr("data-variant")).isEqualTo("danger");
    assertThat(chip.attr("data-size")).isEqualTo("lg");
    assertThat(chip.attr("data-removable")).isEqualTo("true");
    assertThat(chip.attr("data-count")).isEqualTo("3");
    assertThat(chip.selectFirst("b").text()).isEqualTo("Tag:");
    assertThat(chip.attributes().asList()).extracting(a -> a.getKey())
        .doesNotContain("variant", "size", "removable", "count", "label");
  }

  @Test
  void explicitDefaultValueIsOmittedToo() {
    Element chip = render("<sl:test-chip variant=\"neutral\" removable=\"false\">x</sl:test-chip>");

    assertThat(chip.hasAttr("data-variant")).isFalse();
    assertThat(chip.hasAttr("data-removable")).isFalse();
  }

  @Test
  void thPropIsEvaluatedByTheComponentProcessor() {
    Element chip = render("""
            <sl:test-chip th:variant="${admin ? 'danger' : 'success'}" th:removable="${admin}"
                          th:count="${items.size()}" th:label="${'Items: '}">x</sl:test-chip>""",
        Map.of("admin", true, "items", List.of(1, 2)));

    assertThat(chip.attr("data-variant")).isEqualTo("danger");
    assertThat(chip.attr("data-removable")).isEqualTo("true");
    assertThat(chip.attr("data-count")).isEqualTo("2");
    assertThat(chip.selectFirst("b").text()).isEqualTo("Items:");
    assertThat(chip.attributes().asList()).extracting(a -> a.getKey())
        .noneMatch(key -> key.startsWith("th:"));
  }

  @Test
  void thPropWinsOverLiteral() {
    Element chip = render("<sl:test-chip variant=\"danger\" th:variant=\"${'success'}\">x</sl:test-chip>");

    assertThat(chip.attr("data-variant")).isEqualTo("success");
  }

  @Test
  void thPropEvaluatingToNullFallsBackToDefault() {
    Element chip = render("<sl:test-chip th:variant=\"${missing}\">x</sl:test-chip>");

    assertThat(chip.hasAttr("data-variant")).isFalse();
  }

  @Test
  void thAttrSetsAPropBeforeTheComponentReadsIt() {
    // th:attr runs at 700, before the component processor at 1000
    Element chip = render("<sl:test-chip th:attr=\"variant=${'danger'}\">x</sl:test-chip>");

    assertThat(chip.attr("data-variant")).isEqualTo("danger");
    assertThat(chip.hasAttr("variant")).isFalse();
  }

  @Test
  void componentInsideThEachGetsItsOwnProps() {
    Rendered rendered = renderer.render("""
            <div><sl:test-chip th:each="v : ${variants}" th:variant="${v}" th:text="${v}"></sl:test-chip></div>""",
        Map.of("variants", List.of("neutral", "success", "danger")));

    assertThat(rendered.select("span.chip"))
        .extracting(e -> e.attr("data-variant") + "/" + e.text())
        .containsExactly("/neutral", "success/success", "danger/danger");
  }

  // --- sl:attrs ---------------------------------------------------------------------------------

  @Test
  void passesThroughEverythingElseAndMergesClass() {
    Element chip = render("""
        <sl:test-chip variant="success" class="ml-auto extra" id="c1" hx-delete="/orders/42" x-on:click="open = true"
                      aria-describedby="help" data-test="chip" hx-boost>x</sl:test-chip>""");

    assertThat(chip.className()).isEqualTo("chip ml-auto extra");
    assertThat(chip.id()).isEqualTo("c1");
    assertThat(chip.attr("hx-delete")).isEqualTo("/orders/42");
    assertThat(chip.attr("x-on:click")).isEqualTo("open = true");
    assertThat(chip.attr("aria-describedby")).isEqualTo("help");
    assertThat(chip.attr("data-test")).isEqualTo("chip");
    assertThat(chip.hasAttr("hx-boost")).isTrue();
  }

  @Test
  void passedThroughThAttributesAreEvaluatedOnTheRenderedElement() {
    Element chip = render("""
            <sl:test-chip th:text="${label}" th:classappend="${active} ? 'active'" th:hx-post="${url}"
                          th:data-id="${id}">placeholder</sl:test-chip>""",
        Map.of("label", "Saved", "active", true, "url", "/sync", "id", 7));

    assertThat(chip.text()).isEqualTo("Saved");
    assertThat(chip.className()).isEqualTo("chip active");
    assertThat(chip.attr("hx-post")).isEqualTo("/sync");
    assertThat(chip.attr("data-id")).isEqualTo("7");
  }

  @Test
  void passedAttributeOverridesTheTemplatesOwn() {
    Element dot = render("<sl:test-dot data-tone=\"custom\"/>");

    assertThat(dot.attr("data-tone")).isEqualTo("custom");
  }

  @Test
  void undeclaredComponentGetsTheRawAttrsMap() {
    Element raw = render("<sl:test-raw tone=\"loud\" class=\"x\">hi</sl:test-raw>");

    assertThat(raw.tagName()).isEqualTo("em");
    assertThat(raw.attr("data-tone")).isEqualTo("loud");
    assertThat(raw.attr("tone")).isEqualTo("loud");
    assertThat(raw.className()).isEqualTo("raw x");
    assertThat(raw.text()).isEqualTo("hi");
  }

  @Test
  void attrsExpressionLetsATemplateLeaveAttributesOut() {
    Element enabled = render("<sl:test-nav-link href=\"/a\" class=\"x\">A</sl:test-nav-link>");
    Element disabled = render("<sl:test-nav-link disabled href=\"/a\" class=\"x\">A</sl:test-nav-link>");
    Element disabledExpression = render("<sl:test-nav-link disabled th:href=\"@{/a}\">A</sl:test-nav-link>");

    assertThat(enabled.attr("href")).isEqualTo("/a");
    assertThat(disabled.hasAttr("href")).isFalse();
    assertThat(disabled.attr("aria-disabled")).isEqualTo("true");
    assertThat(disabled.className()).isEqualTo("nav-link x");
    assertThat(disabledExpression.hasAttr("href")).isFalse();
  }

  @Test
  void attrsExpressionMustGiveAMap() {
    assertThatThrownBy(() -> render("""
            <sl:test-card><b sl:attrs="${'nope'}">x</b></sl:test-card>"""))
        .rootCause()
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessageContaining("must evaluate to a map");
  }

  @Test
  void undeclaredComponentInADeclaredOnesSlotDoesNotSeeItsProps() {
    Element chip = render("<sl:test-chip variant=\"danger\"><sl:test-props-echo/></sl:test-chip>");

    assertThat(chip.selectFirst("u").text()).isEqualTo("no props");
  }

  // --- loud failures ----------------------------------------------------------------------------

  @Test
  void unknownComponentFailsListingTheRegisteredOnes() {
    assertThatThrownBy(() -> renderer.render("<sl:test-chp>x</sl:test-chp>"))
        .hasStackTraceContaining("Unknown component <sl:test-chp>. Registered components: ")
        .hasStackTraceContaining("test-card, test-chip, test-dot");
  }

  @Test
  void illegalLiteralValueFailsListingTheLegalOnes() {
    assertThatThrownBy(() -> renderer.render("<sl:test-chip variant=\"dangr\">x</sl:test-chip>"))
        .hasStackTraceContaining(
            "Invalid value 'dangr' for prop 'variant' of <sl:test-chip>. Must be one of: neutral, success, danger.");
  }

  @Test
  void illegalExpressionValueFailsToo() {
    assertThatThrownBy(() -> renderer.render("<sl:test-chip th:variant=\"${kind}\">x</sl:test-chip>",
        Map.of("kind", "warning")))
        .hasStackTraceContaining("Invalid value 'warning' for prop 'variant' of <sl:test-chip>.");
  }

  @Test
  void illegalBooleanFails() {
    assertThatThrownBy(() -> renderer.render("<sl:test-chip removable=\"yes\">x</sl:test-chip>"))
        .hasStackTraceContaining("Invalid value 'yes' for boolean prop 'removable' of <sl:test-chip>.");
  }

  @Test
  void failureNamesTheTemplateAndLine() {
    assertThatThrownBy(() -> renderer.render("<div>\n<sl:test-chip variant=\"dangr\">x</sl:test-chip></div>"))
        .rootCause()
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessageContaining("Invalid value 'dangr'")
        .hasMessageContaining("line 2, col 1");
  }

  @Test
  void unlessDefaultOnUndeclaredPropFails() {
    assertThatThrownBy(() -> new Props(
        ComponentDefinition.declared("chip", List.of(), null, "x"),
        Map.of()).unlessDefault("variant"))
        .hasMessage("<sl:chip> has no prop 'variant'. Declared props: .");
  }

  // --- accessible name --------------------------------------------------------------------------

  @Test
  void iconSizedComponentWithoutAccessibleNameFails() {
    assertThatThrownBy(() -> renderer.render("<sl:test-icon-button size=\"icon\">x</sl:test-icon-button>"))
        .hasStackTraceContaining(
            "<sl:test-icon-button size=\"icon\"> needs an accessible name: add aria-label=\"...\" or aria-labelledby=\"...\".");
  }

  @Test
  void blankAriaLabelDoesNotCount() {
    assertThatThrownBy(() -> renderer.render("<sl:test-icon-button size=\"icon\" aria-label=\" \">x</sl:test-icon-button>"))
        .hasStackTraceContaining("needs an accessible name");
  }

  @Test
  void accessibleNameSatisfiesTheRule() {
    assertThat(render("<sl:test-icon-button size=\"icon\" aria-label=\"Delete\">x</sl:test-icon-button>")
        .attr("aria-label")).isEqualTo("Delete");
    assertThat(render("<sl:test-icon-button size=\"icon\" aria-labelledby=\"l1\">x</sl:test-icon-button>")
        .attr("aria-labelledby")).isEqualTo("l1");
    assertThat(render("<sl:test-icon-button size=\"icon\" th:aria-label=\"${'Delete'}\">x</sl:test-icon-button>")
        .attr("aria-label")).isEqualTo("Delete");
  }

  @Test
  void ruleOnlyAppliesToTheDeclaredValues() {
    assertThat(render("<sl:test-icon-button>Save</sl:test-icon-button>").text()).isEqualTo("Save");
  }

  private Element render(String snippet) {
    return render(snippet, Map.of());
  }

  private Element render(String snippet, Map<String, ?> variables) {
    return renderer.render(snippet, variables).root();
  }
}
