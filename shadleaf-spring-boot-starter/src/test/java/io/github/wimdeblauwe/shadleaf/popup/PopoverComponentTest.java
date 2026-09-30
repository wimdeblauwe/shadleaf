package io.github.wimdeblauwe.shadleaf.popup;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PopoverComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void triggerOpensANonModalDialogWithPopovertarget() {
    Rendered rendered = tester.render("""
        <sl:popover id="dimensions">
          <sl:popover-trigger variant="outline">Open</sl:popover-trigger>
          <sl:popover-content>
            <sl:popover-header>
              <sl:popover-title>Dimensions</sl:popover-title>
              <sl:popover-description>Set the dimensions.</sl:popover-description>
            </sl:popover-header>
          </sl:popover-content>
        </sl:popover>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered.document().body().children()).hasSize(2);
    assertThat(rendered).element("button.popover-trigger")
        .hasClass("btn")
        .hasAttribute("popovertarget", "dimensions")
        .hasAttribute("aria-haspopup", "dialog")
        .hasNoAttribute("aria-expanded");
    assertThat(rendered).element("div.popover-content")
        .hasAttribute("id", "dimensions")
        .hasAttribute("role", "dialog")
        .hasAttribute("popover", "")
        .hasAttribute("x-data", "slPopover")
        .hasAttribute("aria-labelledby", "dimensions-title")
        .hasAttribute("aria-describedby", "dimensions-description")
        .hasNoAttribute("aria-modal", "data-side", "data-align");
    assertThat(rendered).element(".popover-title").hasAttribute("id", "dimensions-title").hasNoAttribute("role");
    assertThat(rendered).element("p.popover-description").hasAttribute("id", "dimensions-description");
  }

  @Test
  void needsAnId() {
    assertThatRenderFailure(() -> tester.render("<sl:popover>x</sl:popover>"))
        .hasMessageContaining("<sl:popover> needs id");
  }

  @Test
  void idFromAnExpression() {
    Rendered rendered = tester.render("""
        <sl:popover th:id="|info-${id}|">
          <sl:popover-trigger>Info</sl:popover-trigger>
          <sl:popover-content><sl:popover-title>Info</sl:popover-title></sl:popover-content>
        </sl:popover>""", Map.of("id", 3));

    assertThat(rendered).element(".popover-trigger").hasAttribute("popovertarget", "info-3");
    assertThat(rendered).element(".popover-content").hasAttribute("aria-labelledby", "info-3-title");
  }

  @Test
  void withoutATitleItNeedsAnAriaLabel() {
    assertThat(tester.render("""
        <sl:popover id="p"><sl:popover-content aria-label="Details" side="top" align="end">x</sl:popover-content>
        </sl:popover>""")).element(".popover-content")
        .hasNoAttribute("aria-labelledby", "aria-describedby")
        .hasAttribute("aria-label", "Details")
        .hasAttribute("data-side", "top")
        .hasAttribute("data-align", "end");
  }

  @Test
  void aNestedPopoversTitleIsItsOwn() {
    Rendered rendered = tester.render("""
        <sl:popover id="outer">
          <sl:popover-content aria-label="Outer">
            <sl:popover id="inner"><sl:popover-content><sl:popover-title>Inner</sl:popover-title></sl:popover-content>
            </sl:popover>
          </sl:popover-content>
        </sl:popover>""");

    assertThat(rendered).element("#outer").hasNoAttribute("aria-labelledby");
    assertThat(rendered).element("#inner").hasAttribute("aria-labelledby", "inner-title");
  }

  @Test
  void titleLevel() {
    assertThat(tester.render("<sl:popover-title level=\"2\">T</sl:popover-title>")).root()
        .hasAttribute("role", "heading")
        .hasAttribute("aria-level", "2");
  }
}
