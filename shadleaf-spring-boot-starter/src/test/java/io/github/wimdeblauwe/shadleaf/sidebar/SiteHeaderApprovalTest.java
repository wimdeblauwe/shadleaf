package io.github.wimdeblauwe.shadleaf.sidebar;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:site-header>} alone, with the end slot, and as the inset's page header, rendered into
 * {@code src/test/resources/approved/site-header.approved.html}. The header layout is in {@code sidebar.approved.html}.
 */
class SiteHeaderApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void siteHeader() {
    HtmlApproval.verifyRenders("site-header", tester, List.of(
        "<sl:site-header><a href=\"/\">Acme</a></sl:site-header>",
        "<sl:site-header><a href=\"/\">Acme</a><sl:slot name=\"end\"><button type=\"button\">Search</button></sl:slot></sl:site-header>",
        """
            <sl:sidebar-inset>
              <sl:site-header>
                <sl:sidebar-trigger/>
                <sl:separator orientation="vertical"/>
                <sl:breadcrumb>
                  <sl:breadcrumb-list>
                    <sl:breadcrumb-item><sl:breadcrumb-page>Home</sl:breadcrumb-page></sl:breadcrumb-item>
                  </sl:breadcrumb-list>
                </sl:breadcrumb>
              </sl:site-header>
            </sl:sidebar-inset>"""));
  }
}
