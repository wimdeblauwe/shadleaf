package io.github.wimdeblauwe.shadleaf.alert;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:alert>} and its parts, rendered into {@code src/test/resources/approved/alert.approved.html}.
 */
class AlertApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void variantsAndParts() {
    HtmlApproval.verifyRenders("alert", tester, List.of(
        "<sl:alert>Saved</sl:alert>",
        "<sl:alert variant=\"default\">Saved</sl:alert>",
        "<sl:alert variant=\"destructive\">Not saved</sl:alert>",
        "<sl:alert-title>Heads up</sl:alert-title>",
        "<sl:alert-description>You can add components to your app.</sl:alert-description>",
        """
            <sl:alert variant="destructive" role="alert" id="payment-error">
              <sl:icon name="circle-alert"/>
              <sl:alert-title>Payment failed</sl:alert-title>
              <sl:alert-description><p>Your card was declined.</p></sl:alert-description>
            </sl:alert>"""));
  }
}
