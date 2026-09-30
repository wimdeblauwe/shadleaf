package io.github.wimdeblauwe.shadleaf.disclosure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:accordion>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/accordion.approved.html}. A single accordion gets an explicit {@code name}: the
 * generated one is random.
 */
class AccordionApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void accordionAndParts() {
    HtmlApproval.verifyRenders("accordion", tester, List.of(
        """
            <sl:accordion name="faq">
              <sl:accordion-item open>
                <sl:accordion-trigger>Is it accessible?</sl:accordion-trigger>
                <sl:accordion-content>Yes. It is a details element.</sl:accordion-content>
              </sl:accordion-item>
              <sl:accordion-item>
                <sl:accordion-trigger level="2">Is it styled?</sl:accordion-trigger>
                <sl:accordion-content><p>Yes.</p></sl:accordion-content>
              </sl:accordion-item>
            </sl:accordion>""",
        """
            <sl:accordion type="multiple" class="wide">
              <sl:accordion-item><sl:accordion-trigger>One</sl:accordion-trigger>
                <sl:accordion-content>First</sl:accordion-content></sl:accordion-item>
              <sl:accordion-item open><sl:accordion-trigger>Two</sl:accordion-trigger>
                <sl:accordion-content>Second</sl:accordion-content></sl:accordion-item>
            </sl:accordion>""",
        """
            <sl:accordion name="plans">
              <sl:accordion-item disabled><sl:accordion-trigger>Premium</sl:accordion-trigger>
                <sl:accordion-content>Upgrade to see this.</sl:accordion-content></sl:accordion-item>
              <sl:accordion-item disabled open><sl:accordion-trigger>Current plan</sl:accordion-trigger>
                <sl:accordion-content>Starter</sl:accordion-content></sl:accordion-item>
            </sl:accordion>""",
        "<sl:accordion-item id=\"billing\">x</sl:accordion-item>",
        "<sl:accordion-trigger>Question</sl:accordion-trigger>",
        "<sl:accordion-content>Answer</sl:accordion-content>"));
  }
}
