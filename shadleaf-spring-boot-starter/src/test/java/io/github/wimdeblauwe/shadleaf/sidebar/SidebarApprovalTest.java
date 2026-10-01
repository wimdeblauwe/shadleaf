package io.github.wimdeblauwe.shadleaf.sidebar;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:sidebar-provider>}, {@code <sl:sidebar>}, {@code <sl:sidebar-trigger>} and {@code <sl:sidebar-inset>},
 * alone and composed, rendered into {@code src/test/resources/approved/sidebar.approved.html}. The request sends the
 * {@code sl-sidebar-state} cookie ({@code collapsed}), so the plain provider shows the cookie's state.
 */
class SidebarApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.builder()
      .cookie(SidebarState.COOKIE_NAME, SidebarState.COLLAPSED)
      .build();

  @Test
  void sidebarAndParts() {
    HtmlApproval.verifyRenders("sidebar", tester, List.of(
        "<sl:sidebar-provider>Content</sl:sidebar-provider>",
        "<sl:sidebar-provider state=\"expanded\">Content</sl:sidebar-provider>",
        "<sl:sidebar-provider state=\"collapsed\">Content</sl:sidebar-provider>",
        "<sl:sidebar-provider default-state=\"expanded\">Content</sl:sidebar-provider>",
        "<sl:sidebar>Links</sl:sidebar>",
        "<sl:sidebar side=\"start\">Links</sl:sidebar>",
        "<sl:sidebar side=\"end\">Links</sl:sidebar>",
        "<sl:sidebar collapsible=\"offcanvas\">Links</sl:sidebar>",
        "<sl:sidebar id=\"docs-nav\" aria-label=\"Documentation\">Links</sl:sidebar>",
        "<sl:sidebar-trigger/>",
        "<sl:sidebar-trigger for=\"docs-nav\" variant=\"outline\" size=\"icon\" aria-label=\"Menu\"/>",
        "<sl:sidebar-inset>Content</sl:sidebar-inset>",
        """
            <sl:sidebar-provider>
              <sl:sidebar>
                <ul>
                  <li><a href="/" aria-current="page">Home</a></li>
                  <li><a href="/people">People</a></li>
                </ul>
              </sl:sidebar>
              <sl:sidebar-inset id="main">
                <header><sl:sidebar-trigger/></header>
                <h1>Home</h1>
              </sl:sidebar-inset>
            </sl:sidebar-provider>"""));
  }
}
