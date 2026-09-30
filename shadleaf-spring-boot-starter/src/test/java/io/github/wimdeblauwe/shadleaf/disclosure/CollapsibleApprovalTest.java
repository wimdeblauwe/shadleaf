package io.github.wimdeblauwe.shadleaf.disclosure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:collapsible>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/collapsible.approved.html}.
 */
class CollapsibleApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void collapsibleAndParts() {
    HtmlApproval.verifyRenders("collapsible", tester, List.of(
        """
            <sl:collapsible>
              <sl:collapsible-trigger>Show details</sl:collapsible-trigger>
              <sl:collapsible-content>Details</sl:collapsible-content>
            </sl:collapsible>""",
        """
            <sl:collapsible open class="panel">
              <sl:collapsible-trigger variant="ghost" size="sm">Order #4189</sl:collapsible-trigger>
              <sl:collapsible-content>Shipped</sl:collapsible-content>
            </sl:collapsible>""",
        """
            <sl:collapsible disabled>
              <sl:collapsible-trigger variant="outline" size="icon" aria-label="More">+</sl:collapsible-trigger>
              <sl:collapsible-content>More</sl:collapsible-content>
            </sl:collapsible>""",
        "<sl:collapsible-trigger variant=\"default\">Toggle</sl:collapsible-trigger>",
        "<sl:collapsible-trigger size=\"sm\">Toggle</sl:collapsible-trigger>",
        "<sl:collapsible-content>Content</sl:collapsible-content>"));
  }
}
