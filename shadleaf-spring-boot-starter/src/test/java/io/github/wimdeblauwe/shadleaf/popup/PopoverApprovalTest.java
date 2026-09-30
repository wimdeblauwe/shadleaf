package io.github.wimdeblauwe.shadleaf.popup;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:popover>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/popover.approved.html}.
 */
class PopoverApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void popoverAndParts() {
    HtmlApproval.verifyRenders("popover", tester, List.of(
        """
            <sl:popover id="dimensions">
              <sl:popover-trigger variant="outline">Open popover</sl:popover-trigger>
              <sl:popover-content class="wide">
                <sl:popover-header>
                  <sl:popover-title>Dimensions</sl:popover-title>
                  <sl:popover-description>Set the dimensions for the layer.</sl:popover-description>
                </sl:popover-header>
                <p>Content</p>
              </sl:popover-content>
            </sl:popover>""",
        """
            <sl:popover id="p2">
              <sl:popover-content side="right" align="start" aria-label="Details">Content</sl:popover-content>
            </sl:popover>""",
        "<sl:popover-header>Header</sl:popover-header>",
        "<sl:popover-title>Dimensions</sl:popover-title>",
        "<sl:popover-title level=\"3\">Dimensions</sl:popover-title>",
        "<sl:popover-description>Set the dimensions.</sl:popover-description>"));
  }
}
