package io.github.wimdeblauwe.shadleaf.dialog;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:alert-dialog>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/alert-dialog.approved.html}.
 */
class AlertDialogApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void alertDialogAndParts() {
    HtmlApproval.verifyRenders("alert-dialog", tester, List.of(
        "<sl:alert-dialog id=\"a1\">Content</sl:alert-dialog>",
        "<sl:alert-dialog id=\"a2\" size=\"sm\">Content</sl:alert-dialog>",
        "<sl:alert-dialog id=\"a3\" open>Content</sl:alert-dialog>",
        "<sl:alert-dialog-header>Header</sl:alert-dialog-header>",
        "<sl:alert-dialog-media><sl:icon name=\"circle-alert\"/></sl:alert-dialog-media>",
        "<sl:alert-dialog-media variant=\"destructive\"><sl:icon name=\"trash-2\"/></sl:alert-dialog-media>",
        "<sl:alert-dialog-title>Are you absolutely sure?</sl:alert-dialog-title>",
        "<sl:alert-dialog-title level=\"3\">Are you absolutely sure?</sl:alert-dialog-title>",
        "<sl:alert-dialog-description>This action cannot be undone.</sl:alert-dialog-description>",
        "<sl:alert-dialog-footer>Footer</sl:alert-dialog-footer>",
        "<sl:alert-dialog-cancel/>",
        "<sl:alert-dialog-cancel variant=\"ghost\" size=\"sm\">Keep it</sl:alert-dialog-cancel>",
        """
            <sl:alert-dialog id="delete-account">
              <sl:alert-dialog-header>
                <sl:alert-dialog-title>Are you absolutely sure?</sl:alert-dialog-title>
                <sl:alert-dialog-description>This action cannot be undone.</sl:alert-dialog-description>
              </sl:alert-dialog-header>
              <sl:alert-dialog-footer>
                <sl:alert-dialog-cancel/>
                <sl:button>Continue</sl:button>
              </sl:alert-dialog-footer>
            </sl:alert-dialog>""",
        """
            <sl:alert-dialog id="delete-chat" size="sm">
              <sl:alert-dialog-header>
                <sl:alert-dialog-media variant="destructive"><sl:icon name="trash-2"/></sl:alert-dialog-media>
                <sl:alert-dialog-title>Delete chat?</sl:alert-dialog-title>
              </sl:alert-dialog-header>
              <sl:alert-dialog-footer>
                <sl:alert-dialog-cancel/>
                <sl:button variant="destructive">Delete</sl:button>
              </sl:alert-dialog-footer>
            </sl:alert-dialog>"""));
  }
}
