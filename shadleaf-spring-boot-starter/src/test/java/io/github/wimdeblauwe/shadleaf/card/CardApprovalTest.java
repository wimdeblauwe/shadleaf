package io.github.wimdeblauwe.shadleaf.card;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:card>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/card.approved.html}.
 */
class CardApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void cardAndParts() {
    HtmlApproval.verifyRenders("card", tester, List.of(
        "<sl:card>Content</sl:card>",
        "<sl:card size=\"sm\">Content</sl:card>",
        "<sl:card-header>Header</sl:card-header>",
        "<sl:card-header border>Header</sl:card-header>",
        "<sl:card-title>Orders</sl:card-title>",
        "<sl:card-title level=\"2\">Orders</sl:card-title>",
        "<sl:card-description>Placed this month</sl:card-description>",
        "<sl:card-action>Action</sl:card-action>",
        "<sl:card-content>Content</sl:card-content>",
        "<sl:card-footer>Footer</sl:card-footer>",
        "<sl:card-footer border>Footer</sl:card-footer>",
        """
            <sl:card class="max-w-sm" id="orders">
              <sl:card-header>
                <sl:card-title level="3">Orders</sl:card-title>
                <sl:card-description>Placed this month</sl:card-description>
                <sl:card-action><sl:badge variant="secondary">12</sl:badge></sl:card-action>
              </sl:card-header>
              <sl:card-content><p>Three orders are late.</p></sl:card-content>
              <sl:card-footer><sl:button variant="outline">View all</sl:button></sl:card-footer>
            </sl:card>""",
        """
            <sl:card th:size="${true ? 'sm' : 'default'}">
              <sl:card-title th:text="${'Orders'}">Title</sl:card-title>
            </sl:card>"""));
  }
}
