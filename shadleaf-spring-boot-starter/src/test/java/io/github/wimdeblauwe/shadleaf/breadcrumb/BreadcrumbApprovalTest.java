package io.github.wimdeblauwe.shadleaf.breadcrumb;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The breadcrumb family and {@code <sl:skip-link>}, alone and composed, rendered into
 * {@code src/test/resources/approved/breadcrumb.approved.html}.
 */
class BreadcrumbApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void breadcrumbAndParts() {
    HtmlApproval.verifyRenders("breadcrumb", tester, List.of(
        "<sl:breadcrumb>Levels</sl:breadcrumb>",
        "<sl:breadcrumb aria-label=\"Folders\">Levels</sl:breadcrumb>",
        "<sl:breadcrumb-list>Items</sl:breadcrumb-list>",
        "<sl:breadcrumb-item>Acme</sl:breadcrumb-item>",
        "<sl:breadcrumb-link href=\"/\">Home</sl:breadcrumb-link>",
        "<sl:breadcrumb-page>Breadcrumb</sl:breadcrumb-page>",
        "<sl:breadcrumb-separator/>",
        "<sl:breadcrumb-separator>/</sl:breadcrumb-separator>",
        "<sl:breadcrumb-ellipsis/>",
        """
            <sl:breadcrumb-ellipsis>
              <sl:dropdown-menu-item as="a" href="/docs">Documentation</sl:dropdown-menu-item>
              <sl:dropdown-menu-item as="a" href="/themes">Themes</sl:dropdown-menu-item>
            </sl:breadcrumb-ellipsis>""",
        """
            <sl:breadcrumb-ellipsis id="path-more" aria-label="Folders above">
              <sl:dropdown-menu-item as="a" href="/docs">Documentation</sl:dropdown-menu-item>
            </sl:breadcrumb-ellipsis>""",
        """
            <sl:breadcrumb>
              <sl:breadcrumb-list>
                <sl:breadcrumb-item><sl:breadcrumb-link href="/">Home</sl:breadcrumb-link></sl:breadcrumb-item>
                <sl:breadcrumb-separator/>
                <sl:breadcrumb-item><sl:breadcrumb-link href="/components">Components</sl:breadcrumb-link></sl:breadcrumb-item>
                <sl:breadcrumb-separator/>
                <sl:breadcrumb-item><sl:breadcrumb-page>Breadcrumb</sl:breadcrumb-page></sl:breadcrumb-item>
              </sl:breadcrumb-list>
            </sl:breadcrumb>""",
        "<sl:skip-link/>",
        "<sl:skip-link for=\"content\">Skip to the list</sl:skip-link>"));
  }
}
