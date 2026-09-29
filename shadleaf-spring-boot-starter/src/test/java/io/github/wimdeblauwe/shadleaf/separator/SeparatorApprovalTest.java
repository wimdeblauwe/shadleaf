package io.github.wimdeblauwe.shadleaf.separator;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:separator>} in both orientations, decorative and not, rendered into
 * {@code src/test/resources/approved/separator.approved.html}.
 */
class SeparatorApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void orientations() {
    HtmlApproval.verifyRenders("separator", tester, List.of(
        "<sl:separator/>",
        "<sl:separator orientation=\"horizontal\"/>",
        "<sl:separator orientation=\"vertical\"/>",
        "<sl:separator decorative=\"false\"/>",
        "<sl:separator orientation=\"vertical\" decorative=\"false\"/>",
        "<sl:separator class=\"my-4\" id=\"divider\"/>"));
  }
}
