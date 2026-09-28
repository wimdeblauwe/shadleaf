package io.github.wimdeblauwe.shadleaf.icon;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The markup {@code <sl:icon>} emits, approved in {@code src/test/resources/approved/icon.approved.html}. The paths
 * are included: they come from the pinned lucide version, so bumping it shows up here. See {@link HtmlApproval}.
 */
class IconApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void attributesAndStates() {
    List<String> snippets = List.of(
        "<sl:icon name=\"trash\"/>",
        "<sl:icon name=\"trash-2\"/>",
        "<sl:icon name=\"plus\" size=\"16\"/>",
        "<sl:icon name=\"plus\" stroke-width=\"1.5\"/>",
        "<sl:icon name=\"trash\" label=\"Delete\"/>",
        "<sl:icon name=\"x\" class=\"text-muted\" data-test=\"close\"/>");

    StringBuilder approval = new StringBuilder();
    for (String snippet : snippets) {
      approval.append("<!-- ").append(snippet).append(" -->\n")
          .append(tester.render(snippet).normalizedHtml()).append("\n\n");
    }

    HtmlApproval.verify("icon", approval.toString());
  }
}
