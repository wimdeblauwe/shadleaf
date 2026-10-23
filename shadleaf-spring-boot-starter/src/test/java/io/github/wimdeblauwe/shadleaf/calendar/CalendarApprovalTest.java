package io.github.wimdeblauwe.shadleaf.calendar;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:calendar>}, rendered into {@code src/test/resources/approved/calendar.approved.html}. The behaviour with
 * {@code th:field} and the locale is in {@link CalendarBindingTest}.
 */
class CalendarApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void calendar() {
    List<String> snippets = new ArrayList<>();
    for (String layout : LibraryComponents.registry().get("calendar").prop("caption-layout").values()) {
      snippets.add("<sl:calendar caption-layout=\"%s\" name=\"due\" aria-label=\"Due\"/>".formatted(layout));
    }
    snippets.addAll(List.of(
        """
            <sl:calendar name="due" value="2026-10-23" min="2026-01-01" max="2026-12-31" required class="w-full"
                         number-of-months="2" show-outside-days="false" show-week-number week-starts-on="1"
                         default-month="2026-09" start-month="2026-01" end-month="2026-12"
                         disabled-dates="sat,sun,2026-12-24..2026-12-31" hx-post="/due" hx-trigger="change"/>""",
        """
            <sl:calendar aria-label="Stay" class="w-full" week-starts-on="0">
              <sl:input type="date" name="from" value="2026-10-12" aria-label="Check-in"/>
              <sl:input type="date" name="to" value="2026-10-16" aria-label="Check-out"/>
            </sl:calendar>"""));

    HtmlApproval.verifyRenders("calendar", tester, snippets);
  }
}
