package io.github.wimdeblauwe.shadleaf.theme;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:theme-toggle>} as a menu and as a button, rendered into
 * {@code src/test/resources/approved/theme-toggle.approved.html}.
 */
class ThemeToggleApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void menuAndButton() {
    HtmlApproval.verifyRenders("theme-toggle", tester, List.of(
        "<sl:theme-toggle/>",
        "<sl:theme-toggle id=\"footer-theme\" variant=\"ghost\" size=\"icon-sm\" side=\"top\" align=\"start\"/>",
        "<sl:theme-toggle as=\"button\"/>",
        "<sl:theme-toggle as=\"button\" id=\"dark-mode\" variant=\"ghost\" size=\"icon-lg\" aria-label=\"Night mode\"/>"));
  }
}
