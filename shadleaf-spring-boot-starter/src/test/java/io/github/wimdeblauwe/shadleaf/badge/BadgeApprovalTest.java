package io.github.wimdeblauwe.shadleaf.badge;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every variant of {@code <sl:badge>}, as a span and as a link, with and without icons, rendered into
 * {@code src/test/resources/approved/badge.approved.html}. The variants come from the badge's {@code <sl:props>}.
 */
class BadgeApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void variantsAndElements() {
    List<String> snippets = new ArrayList<>();
    for (String variant : LibraryComponents.registry().get("badge").prop("variant").values()) {
      snippets.add("<sl:badge variant=\"%s\">New</sl:badge>".formatted(variant));
      snippets.add("<sl:badge as=\"a\" href=\"/orders?status=open\" variant=\"%s\">Open</sl:badge>".formatted(variant));
    }
    snippets.addAll(List.of(
        "<sl:badge/>",
        "<sl:badge variant=\"secondary\"><sl:slot name=\"icon-start\"><sl:icon name=\"badge-check\"/></sl:slot>Verified</sl:badge>",
        "<sl:badge variant=\"outline\">3 items<sl:slot name=\"icon-end\"><sl:icon name=\"arrow-right\"/></sl:slot></sl:badge>",
        "<sl:badge class=\"ms-2\" id=\"status\" data-testid=\"status\">Shipped</sl:badge>"));

    StringBuilder approval = new StringBuilder();
    for (String snippet : snippets) {
      Rendered rendered = tester.render(snippet);
      rendered.select("svg > *").remove();
      approval.append("<!-- ").append(snippet).append(" -->\n")
          .append(rendered.normalizedHtml()).append("\n\n");
    }

    HtmlApproval.verify("badge", approval.toString());
  }
}
