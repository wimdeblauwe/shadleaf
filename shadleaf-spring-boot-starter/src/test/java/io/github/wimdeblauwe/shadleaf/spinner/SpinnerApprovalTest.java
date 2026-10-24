package io.github.wimdeblauwe.shadleaf.spinner;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:spinner>}, rendered into {@code src/test/resources/approved/spinner.approved.html}.
 */
class SpinnerApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void spinner() {
    HtmlApproval.verifyRenders("spinner", tester, List.of(
        "<sl:spinner/>",
        "<sl:spinner size=\"32\"/>",
        "<sl:spinner aria-label=\"Saving\" id=\"save-spinner\"/>"));
  }
}
