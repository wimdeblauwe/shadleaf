package io.github.wimdeblauwe.shadleaf.disclosure;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.junit.jupiter.api.Test;

class CollapsibleComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void aDetailsElementWithAPlainSummary() {
    Rendered rendered = tester.render("""
        <sl:collapsible id="more">
          <sl:collapsible-trigger>Show more</sl:collapsible-trigger>
          <sl:collapsible-content>More</sl:collapsible-content>
        </sl:collapsible>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("details").hasClass("collapsible").hasAttribute("id", "more")
        .hasNoAttribute("open", "name", "x-data");
    assertThat(rendered.root().child(0)).hasTag("summary").hasClass("collapsible-trigger")
        .hasNoAttribute("data-variant", "data-size");
    assertThat(rendered.root().child(0).classNames()).doesNotContain("btn");
    assertThat(rendered).element("div.collapsible-content").hasText("More");
  }

  @Test
  void aVariantOrSizeDrawsTheTriggerAsAButton() {
    assertThat(tester.render("<sl:collapsible-trigger variant=\"ghost\" class=\"w-full\">x</sl:collapsible-trigger>"))
        .root().hasClass("btn", "collapsible-trigger", "w-full").hasAttribute("data-variant", "ghost")
        .hasNoAttribute("data-size");
    assertThat(tester.render("<sl:collapsible-trigger size=\"sm\">x</sl:collapsible-trigger>"))
        .root().hasClass("btn").hasAttribute("data-size", "sm").hasNoAttribute("data-variant");
    assertThat(tester.render("<sl:collapsible-trigger variant=\"default\" size=\"default\">x</sl:collapsible-trigger>"))
        .root().hasClass("btn").hasNoAttribute("data-size", "data-variant");
  }

  @Test
  void anIconTriggerNeedsALabel() {
    assertThatRenderFailure(() -> tester.render("<sl:collapsible-trigger size=\"icon\">+</sl:collapsible-trigger>"))
        .hasMessageContaining("aria-label");
  }

  @Test
  void disabled() {
    Rendered closed = tester.render("""
        <sl:collapsible disabled><sl:collapsible-trigger>T</sl:collapsible-trigger>
          <sl:collapsible-content>C</sl:collapsible-content></sl:collapsible>""");
    assertThat(closed).root().hasAttribute("data-disabled", "true");
    assertThat(closed).element("summary").hasAttribute("aria-disabled", "true").hasAttribute("tabindex", "-1");
    assertThat(closed).hasNoElement(".collapsible-content");

    Rendered open = tester.render("""
        <sl:collapsible disabled open><sl:collapsible-trigger>T</sl:collapsible-trigger>
          <sl:collapsible-content>C</sl:collapsible-content></sl:collapsible>""");
    assertThat(open).root().hasAttribute("open");
    assertThat(open).element(".collapsible-content").hasText("C");
  }
}
