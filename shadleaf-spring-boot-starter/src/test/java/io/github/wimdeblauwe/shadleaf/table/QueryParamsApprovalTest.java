package io.github.wimdeblauwe.shadleaf.table;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:query-params>}, rendered into {@code src/test/resources/approved/query-params.approved.html}, in a
 * request searched, sorted twice, sized and on its third page.
 */
class QueryParamsApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.builder()
      .requestUri("/people?q=ada+l%C3%A9&sort=name,desc&sort=email&page=2&size=50&members_page=1")
      .build();

  @Test
  void queryParams() {
    HtmlApproval.verifyRenders("query-params", tester, List.of(
        "<form><sl:query-params/></form>",
        "<form><sl:query-params except=\"q\"/></form>",
        "<form><sl:query-params except=\"q,size\" qualifier=\"members\"/></form>",
        "<div><sl:query-params except=\"q\" form=\"people-search\"/></div>"));
  }
}
