package io.github.wimdeblauwe.shadleaf.tabs;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:tabs>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/tabs.approved.html}.
 */
class TabsApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void tabsAndParts() {
    HtmlApproval.verifyRenders("tabs", tester, List.of(
        """
            <sl:tabs id="settings" active="account">
              <sl:tabs-list aria-label="Settings">
                <sl:tabs-trigger tab="account">Account</sl:tabs-trigger>
                <sl:tabs-trigger tab="password">Password</sl:tabs-trigger>
                <sl:tabs-trigger tab="billing" disabled>Billing</sl:tabs-trigger>
              </sl:tabs-list>
              <sl:tabs-content tab="account">Account settings</sl:tabs-content>
              <sl:tabs-content tab="password">Password settings</sl:tabs-content>
              <sl:tabs-content tab="billing">Billing settings</sl:tabs-content>
            </sl:tabs>""",
        """
            <sl:tabs id="report" active="analytics" orientation="vertical" activation-mode="manual" class="wide">
              <sl:tabs-list variant="line">
                <sl:tabs-trigger tab="overview" href="?tab=overview">Overview</sl:tabs-trigger>
                <sl:tabs-trigger tab="analytics" href="?tab=analytics">Analytics</sl:tabs-trigger>
                <sl:tabs-trigger tab="reports" href="?tab=reports" disabled>Reports</sl:tabs-trigger>
              </sl:tabs-list>
              <sl:tabs-content tab="overview">Overview</sl:tabs-content>
              <sl:tabs-content tab="analytics" hx-get="/analytics" hx-trigger="sl-tabs-show once">…</sl:tabs-content>
              <sl:tabs-content tab="reports">Reports</sl:tabs-content>
            </sl:tabs>""",
        "<sl:tabs-list>x</sl:tabs-list>",
        "<sl:tabs-trigger tab=\"x\">X</sl:tabs-trigger>",
        "<sl:tabs-content tab=\"x\">X</sl:tabs-content>"));
  }
}
