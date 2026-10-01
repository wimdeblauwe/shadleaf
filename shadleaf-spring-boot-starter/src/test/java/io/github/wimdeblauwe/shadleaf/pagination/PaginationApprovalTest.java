package io.github.wimdeblauwe.shadleaf.pagination;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

/**
 * The pagination family, {@code <sl:pagination-summary>} and {@code <sl:pagination-size>}, rendered into
 * {@code src/test/resources/approved/pagination.approved.html}, on page 5 of 27 in a request with a filter and a sort.
 */
class PaginationApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.builder()
      .requestUri("/people?q=ada&sort=name,asc&page=4&size=10")
      .build();

  @Test
  void paginationAndParts() {
    Map<String, Object> variables = Map.of(
        "people", new PageImpl<>(Collections.nCopies(10, "row"), PageRequest.of(4, 10, Sort.by("name")), 270),
        "first", new PageImpl<>(Collections.nCopies(10, "row"), PageRequest.of(0, 10), 30),
        "slice", new SliceImpl<>(Collections.nCopies(10, "row"), PageRequest.of(1, 10), true));
    HtmlApproval.verifyRenders("pagination", tester, List.of(
        "<sl:pagination th:page=\"${people}\"/>",
        "<sl:pagination th:page=\"${first}\"/>",
        "<sl:pagination th:page=\"${slice}\"/>",
        "<sl:pagination current=\"2\" total=\"3\" qualifier=\"members\"/>",
        """
            <sl:pagination>
              <sl:pagination-content>
                <sl:pagination-item><sl:pagination-previous href="?page=1"/></sl:pagination-item>
                <sl:pagination-item><sl:pagination-link href="?page=1">1</sl:pagination-link></sl:pagination-item>
                <sl:pagination-item><sl:pagination-link href="?page=2" active>2</sl:pagination-link></sl:pagination-item>
                <sl:pagination-item><sl:pagination-ellipsis/></sl:pagination-item>
                <sl:pagination-item><sl:pagination-next disabled/></sl:pagination-item>
              </sl:pagination-content>
            </sl:pagination>""",
        "<sl:pagination-summary th:page=\"${people}\"/>",
        "<sl:pagination-summary th:page=\"${people}\" format=\"page\"/>",
        "<sl:pagination-summary th:page=\"${slice}\"/>",
        "<sl:pagination-size th:page=\"${people}\"/>"), variables);
  }
}
