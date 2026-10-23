package io.github.wimdeblauwe.shadleaf.calendar;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:date-picker>}, rendered into {@code src/test/resources/approved/date-picker.approved.html}. The behaviour
 * with {@code th:field} and the locale is in {@link DatePickerBindingTest}.
 */
class DatePickerApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void datePicker() {
    List<String> snippets = new ArrayList<>();
    snippets.add("<sl:date-picker popover-id=\"due-picker\" name=\"due\" aria-label=\"Due\"/>");
    for (String format : LibraryComponents.registry().get("date-picker").prop("format").values()) {
      snippets.add("<sl:date-picker popover-id=\"due-picker\" format=\"%s\" name=\"due\" value=\"2026-10-23\" aria-label=\"Due\"/>"
          .formatted(format));
    }
    snippets.addAll(List.of(
        """
            <sl:date-picker popover-id="due-picker" placeholder="Choose the due date" name="due" aria-label="Due"
                            required min="2026-01-01" class="w-full" caption-layout="dropdown" disabled-dates="sat,sun"/>""",
        """
            <sl:date-picker popover-id="closed-picker" name="closed" value="2026-10-23" aria-label="Closed on" disabled/>""",
        """
            <sl:date-picker popover-id="stay-picker" aria-label="Stay" number-of-months="2">
              <sl:input type="date" name="from" value="2026-10-12" aria-label="Check-in"/>
              <sl:input type="date" name="to" value="2026-10-16" aria-label="Check-out"/>
            </sl:date-picker>"""));

    HtmlApproval.verifyRenders("date-picker", tester, snippets);
  }
}
