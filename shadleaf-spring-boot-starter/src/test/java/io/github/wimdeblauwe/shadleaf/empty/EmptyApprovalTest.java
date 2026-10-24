package io.github.wimdeblauwe.shadleaf.empty;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:empty>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/empty.approved.html}.
 */
class EmptyApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void emptyAndParts() {
    HtmlApproval.verifyRenders("empty", tester, List.of(
        "<sl:empty>Content</sl:empty>",
        "<sl:empty border>Content</sl:empty>",
        "<sl:empty-header>Header</sl:empty-header>",
        "<sl:empty-media><sl:icon name=\"folder\"/></sl:empty-media>",
        "<sl:empty-media variant=\"icon\"><sl:icon name=\"folder\"/></sl:empty-media>",
        "<sl:empty-title>No projects yet</sl:empty-title>",
        "<sl:empty-title level=\"2\">No projects yet</sl:empty-title>",
        "<sl:empty-description>Create your first project.</sl:empty-description>",
        "<sl:empty-content>Content</sl:empty-content>",
        """
            <sl:empty border id="projects-empty">
              <sl:empty-header>
                <sl:empty-media variant="icon"><sl:icon name="folder-code"/></sl:empty-media>
                <sl:empty-title level="2">No projects yet</sl:empty-title>
                <sl:empty-description>Get started by creating your first project.</sl:empty-description>
              </sl:empty-header>
              <sl:empty-content><sl:button>Create project</sl:button></sl:empty-content>
            </sl:empty>""",
        """
            <sl:empty th:border="${true}">
              <sl:empty-title th:level="${3}">[[${'No results'}]]</sl:empty-title>
            </sl:empty>"""));
  }
}
