package io.github.wimdeblauwe.shadleaf.skeleton;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:skeleton>} in each shape, rendered into {@code src/test/resources/approved/skeleton.approved.html}.
 */
class SkeletonApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void shapes() {
    HtmlApproval.verifyRenders("skeleton", tester, List.of(
        "<sl:skeleton/>",
        "<sl:skeleton shape=\"line\"/>",
        "<sl:skeleton shape=\"circle\"/>",
        "<sl:skeleton class=\"h-32 w-64\" id=\"chart-placeholder\"/>"));
  }
}
