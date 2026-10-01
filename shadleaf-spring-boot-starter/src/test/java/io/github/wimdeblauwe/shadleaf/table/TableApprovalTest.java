package io.github.wimdeblauwe.shadleaf.table;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:table>}, {@code <sl:table-head>}, {@code <sl:table-empty>}, {@code <sl:table-select-all>} and
 * {@code <sl:table-selection-count>}, rendered into
 * {@code src/test/resources/approved/table.approved.html}, in a request sorted by name.
 */
class TableApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.builder()
      .requestUri("/people?q=ada&sort=name,desc&page=2")
      .build();

  @Test
  void tableAndParts() {
    HtmlApproval.verifyRenders("table", tester, List.of(
        "<sl:table><tbody><tr><td>Ada</td></tr></tbody></sl:table>",
        "<sl:table id=\"people\" class=\"extra\"><tbody><tr><td>Ada</td></tr></tbody></sl:table>",
        "<sl:table-head>Name</sl:table-head>",
        "<sl:table-head align=\"center\">Status</sl:table-head>",
        "<sl:table-head align=\"end\">Amount</sl:table-head>",
        "<sl:table-head sort=\"name\">Name</sl:table-head>",
        "<sl:table-head sort=\"email\">Email</sl:table-head>",
        "<sl:table-head sort=\"amount\" align=\"end\">Amount</sl:table-head>",
        "<sl:table-empty colspan=\"3\"/>",
        "<sl:table-empty colspan=\"3\">Nobody matches.</sl:table-empty>",
        "<sl:table-select-all/>",
        "<sl:table-select-all name=\"ids\" id=\"people-select-all\" form=\"bulk\" class=\"extra\"/>",
        "<sl:table-select-all aria-label=\"Select all people on this page\"/>",
        "<sl:table-selection-count table=\"people\"/>",
        "<sl:table-selection-count table=\"people\" id=\"people-count\" class=\"extra\"/>",
        """
            <sl:table qualifier="members">
              <caption>Members</caption>
              <thead>
                <tr>
                  <sl:table-head sort="name">Name</sl:table-head>
                  <sl:table-head align="end">Orders</sl:table-head>
                </tr>
              </thead>
              <tbody>
                <tr><td>Ada Lovelace</td><td data-align="end">3</td></tr>
              </tbody>
              <tfoot>
                <tr><td>Total</td><td data-align="end">3</td></tr>
              </tfoot>
            </sl:table>"""));
  }
}
