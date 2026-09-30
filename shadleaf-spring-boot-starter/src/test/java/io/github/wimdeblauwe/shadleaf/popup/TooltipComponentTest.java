package io.github.wimdeblauwe.shadleaf.popup;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.junit.jupiter.api.Test;

class TooltipComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void wrapsTheTriggerAndAManualPopover() {
    Rendered rendered = tester.render("""
        <sl:tooltip>
          <sl:button variant="outline">Hover</sl:button>
          <sl:tooltip-content>Add to library</sl:tooltip-content>
        </sl:tooltip>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("span")
        .hasClassName("tooltip")
        .hasAttribute("x-data", "slTooltip")
        .hasNoAttribute("data-delay");
    assertThat(rendered.root().child(0)).hasTag("button").hasText("Hover");
    assertThat(rendered).element(".tooltip > span.tooltip-content")
        .hasAttribute("role", "tooltip")
        .hasAttribute("popover", "manual")
        .hasNoAttribute("id", "data-side", "data-align")
        .element("span.tooltip-arrow[aria-hidden=true]");
  }

  @Test
  void delaySideAlignAndOwnAttributes() {
    Rendered rendered = tester.render("""
        <sl:tooltip delay="0" class="tip">
          <a href="/help">Help</a>
          <sl:tooltip-content side="bottom" align="start" id="help-tip">Opens the manual</sl:tooltip-content>
        </sl:tooltip>""");

    assertThat(rendered).root().hasAttribute("data-delay", "0").hasAttribute("class", "tooltip tip");
    assertThat(rendered).element(".tooltip-content")
        .hasAttribute("id", "help-tip")
        .hasAttribute("data-side", "bottom")
        .hasAttribute("data-align", "start");
  }
}
