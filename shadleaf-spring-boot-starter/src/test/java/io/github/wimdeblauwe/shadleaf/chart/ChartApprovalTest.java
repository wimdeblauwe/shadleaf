package io.github.wimdeblauwe.shadleaf.chart;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:chart>} and its parts, rendered into {@code src/test/resources/approved/chart.approved.html}: every value
 * of their enum props. The data block and the checks are in {@link ChartComponentTest}.
 */
class ChartApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void chart() {
    List<String> snippets = new ArrayList<>();
    snippets.add("""
        <sl:chart th:data="${ {} }" label="Visitors, January to June" margin="12" time-zone="Europe/Brussels" id="visitors"
                  class="mt-4">
          <sl:chart-grid vertical/>
          <sl:chart-x-axis key="month" tick-margin="10" min-tick-gap="32"/>
          <sl:chart-y-axis tick-margin="4" tick-count="3"/>
          <sl:chart-tooltip/>
          <sl:chart-legend/>
          <sl:chart-bar key="desktop" label="Desktop" color="var(--chart-1)" color-dark="var(--chart-3)" radius="4"/>
          <sl:chart-line key="mobile" label="Mobile" curve="linear" dots dashed join="desktop" stroke-width="3"/>
          <sl:chart-area key="tablet" label="Tablet" stack="a" dashed join="desktop" fill-opacity="0.8"/>
        </sl:chart>""");
    for (String format : LibraryComponents.registry().get("chart-x-axis").prop("format").values()) {
      snippets.add("<sl:chart-x-axis key=\"day\" format=\"%s\" currency=\"EUR\"/>".formatted(format));
    }
    for (String format : LibraryComponents.registry().get("chart-y-axis").prop("format").values()) {
      snippets.add("<sl:chart-y-axis format=\"%s\" currency=\"USD\"/>".formatted(format));
    }
    for (String indicator : LibraryComponents.registry().get("chart-tooltip").prop("indicator").values()) {
      snippets.add("<sl:chart-tooltip indicator=\"%s\"/>".formatted(indicator));
    }
    for (String format : LibraryComponents.registry().get("chart-tooltip").prop("value-format").values()) {
      snippets.add("<sl:chart-tooltip label-format=\"%s\" value-format=\"%s\" currency=\"EUR\"/>"
          .formatted(format, format));
    }
    snippets.add("<sl:chart-tooltip cursor=\"false\" hide-label hide-indicator label-key=\"monthLabel\"/>");
    for (String align : LibraryComponents.registry().get("chart-legend").prop("vertical-align").values()) {
      snippets.add("<sl:chart-legend vertical-align=\"%s\"/>".formatted(align));
    }
    snippets.add("<sl:chart-bar key=\"desktop\" stack=\"a\" radius=\"0,0,4,4\"/>");
    for (String curve : LibraryComponents.registry().get("chart-line").prop("curve").values()) {
      snippets.add("<sl:chart-line key=\"desktop\" curve=\"%s\"/>".formatted(curve));
    }
    for (String curve : LibraryComponents.registry().get("chart-area").prop("curve").values()) {
      snippets.add("<sl:chart-area key=\"desktop\" curve=\"%s\"/>".formatted(curve));
    }

    HtmlApproval.verifyRenders("chart", tester, snippets);
  }
}
