package io.github.wimdeblauwe.shadleaf.toast;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:toaster>} and {@code <sl:toast>}, rendered into {@code src/test/resources/approved/toast.approved.html}.
 */
class ToastApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void toasterAndToasts() {
    HtmlApproval.verifyRenders("toast", tester, List.of(
        """
            <sl:toaster>
              <sl:toast title="Event created" description="Monday, January 3rd at 6:00pm"/>
            </sl:toaster>""",
        "<sl:toaster id=\"notes\" position=\"top-left\" duration=\"0\" visible-toasts=\"1\"/>",
        "<sl:toaster position=\"top-center\"/>",
        "<sl:toaster position=\"top-right\"/>",
        "<sl:toaster position=\"bottom-left\"/>",
        "<sl:toaster position=\"bottom-center\"/>",
        "<sl:toast variant=\"success\" title=\"Saved\"/>",
        "<sl:toast variant=\"info\" title=\"Heads up\" duration=\"10000\"/>",
        "<sl:toast variant=\"warning\" title=\"Almost full\" description=\"90% of your storage is used.\"/>",
        "<sl:toast variant=\"error\" title=\"Could not save\"/>",
        """
            <sl:toast title="Invitation sent" class="invite">
              <sl:slot name="icon"><sl:icon name="mail"/></sl:slot>
              Sent to <a href="/members">the new member</a>.
            </sl:toast>"""));
  }
}
