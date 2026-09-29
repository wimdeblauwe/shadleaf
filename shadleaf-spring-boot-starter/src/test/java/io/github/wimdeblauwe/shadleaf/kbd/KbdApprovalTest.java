package io.github.wimdeblauwe.shadleaf.kbd;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:kbd>} and {@code <sl:kbd-group>}, rendered into {@code src/test/resources/approved/kbd.approved.html}.
 */
class KbdApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void keysAndCombinations() {
    HtmlApproval.verifyRenders("kbd", tester, List.of(
        "<sl:kbd>Esc</sl:kbd>",
        "<sl:kbd><sl:icon name=\"command\" label=\"Command\"/></sl:kbd>",
        "<sl:kbd-group><sl:kbd>Ctrl</sl:kbd>+<sl:kbd>K</sl:kbd></sl:kbd-group>",
        "<sl:kbd class=\"ms-auto\" id=\"shortcut\">/</sl:kbd>"));
  }
}
