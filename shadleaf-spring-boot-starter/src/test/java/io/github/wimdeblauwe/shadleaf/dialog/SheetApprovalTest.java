package io.github.wimdeblauwe.shadleaf.dialog;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:sheet>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/sheet.approved.html}.
 */
class SheetApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void sheetAndParts() {
    HtmlApproval.verifyRenders("sheet", tester, List.of(
        "<sl:sheet id=\"s1\">Content</sl:sheet>",
        "<sl:sheet id=\"s2\" side=\"top\">Content</sl:sheet>",
        "<sl:sheet id=\"s3\" side=\"right\">Content</sl:sheet>",
        "<sl:sheet id=\"s4\" side=\"bottom\">Content</sl:sheet>",
        "<sl:sheet id=\"s5\" side=\"left\">Content</sl:sheet>",
        "<sl:sheet id=\"s6\" open>Content</sl:sheet>",
        "<sl:sheet id=\"s7\" close-button=\"false\">Content</sl:sheet>",
        "<sl:sheet-header>Header</sl:sheet-header>",
        "<sl:sheet-title>Edit profile</sl:sheet-title>",
        "<sl:sheet-title level=\"3\">Edit profile</sl:sheet-title>",
        "<sl:sheet-description>Make changes to your profile.</sl:sheet-description>",
        "<sl:sheet-footer>Footer</sl:sheet-footer>",
        """
            <sl:sheet id="edit-profile">
              <sl:sheet-header>
                <sl:sheet-title>Edit profile</sl:sheet-title>
                <sl:sheet-description>Make changes to your profile.</sl:sheet-description>
              </sl:sheet-header>
              <p>Content</p>
              <sl:sheet-footer close-button>
                <sl:button type="submit">Save changes</sl:button>
              </sl:sheet-footer>
            </sl:sheet>"""));
  }
}
