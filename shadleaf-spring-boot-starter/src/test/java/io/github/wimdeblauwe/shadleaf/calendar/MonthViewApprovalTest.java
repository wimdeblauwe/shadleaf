package io.github.wimdeblauwe.shadleaf.calendar;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:month-view>}, rendered into {@code src/test/resources/approved/month-view.approved.html}. The month, the
 * slot, the links and the request are in {@link MonthViewComponentTest}.
 */
class MonthViewApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.builder().requestUri("/agenda?team=ops").build();

  @Test
  void monthView() {
    HtmlApproval.verifyRenders("month-view", tester, List.of(
        "<sl:month-view month=\"2026-10\" today=\"2026-10-23\"/>",
        """
            <sl:month-view month="2026-10" today="2026-10-23" day-href="/agenda/{date}" class="w-full">
              <sl:slot name="day"><sl:badge th:if="${slDay.number == 12}">Release</sl:badge></sl:slot>
            </sl:month-view>""",
        """
            <sl:month-view month="2026-02" today="2026-10-23" month-param="rota" show-outside-days="false"
                           show-week-number week-starts-on="1"/>""",
        "<sl:month-view month=\"2026-10\" today=\"2026-10-23\" nav-size=\"icon-lg\"/>"));
  }
}
