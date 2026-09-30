package io.github.wimdeblauwe.shadleaf.popup;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:tooltip>} and its content, rendered into {@code src/test/resources/approved/tooltip.approved.html}.
 */
class TooltipApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void tooltipAndContent() {
    HtmlApproval.verifyRenders("tooltip", tester, List.of(
        """
            <sl:tooltip>
              <sl:button variant="outline">Hover</sl:button>
              <sl:tooltip-content>Add to library</sl:tooltip-content>
            </sl:tooltip>""",
        """
            <sl:tooltip delay="0" class="toolbar-tip">
              <sl:button variant="ghost" size="icon" aria-label="Save"><sl:icon name="save"/></sl:button>
              <sl:tooltip-content side="bottom" align="start" id="save-tip">Save
                <sl:kbd-group><sl:kbd>Ctrl</sl:kbd><sl:kbd>S</sl:kbd></sl:kbd-group></sl:tooltip-content>
            </sl:tooltip>""",
        "<sl:tooltip-content side=\"right\">Text</sl:tooltip-content>",
        "<sl:tooltip-content side=\"left\" align=\"end\">Text</sl:tooltip-content>"));
  }
}
