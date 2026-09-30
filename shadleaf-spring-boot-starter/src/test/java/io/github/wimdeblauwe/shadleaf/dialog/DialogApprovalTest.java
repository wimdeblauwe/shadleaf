package io.github.wimdeblauwe.shadleaf.dialog;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:dialog>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/dialog.approved.html}.
 */
class DialogApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void dialogAndParts() {
    HtmlApproval.verifyRenders("dialog", tester, List.of(
        "<sl:dialog id=\"d1\">Content</sl:dialog>",
        "<sl:dialog id=\"d2\" open>Content</sl:dialog>",
        "<sl:dialog id=\"d3\" close-button=\"false\">Content</sl:dialog>",
        "<sl:dialog-header>Header</sl:dialog-header>",
        "<sl:dialog-title>Edit profile</sl:dialog-title>",
        "<sl:dialog-title level=\"3\">Edit profile</sl:dialog-title>",
        "<sl:dialog-description>Make changes to your profile.</sl:dialog-description>",
        "<sl:dialog-footer>Footer</sl:dialog-footer>",
        """
            <sl:dialog id="edit-profile" class="wide">
              <sl:dialog-header>
                <sl:dialog-title>Edit profile</sl:dialog-title>
                <sl:dialog-description>Make changes to your profile.</sl:dialog-description>
              </sl:dialog-header>
              <p>Content</p>
              <sl:dialog-footer close-button>
                <sl:button type="submit">Save changes</sl:button>
              </sl:dialog-footer>
            </sl:dialog>""",
        """
            <sl:dialog th:id="|delete-${42}|" aria-label="Delete order">
              <p>Delete order 42?</p>
            </sl:dialog>"""));
  }
}
