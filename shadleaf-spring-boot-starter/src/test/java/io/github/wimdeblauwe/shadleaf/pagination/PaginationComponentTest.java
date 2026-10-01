package io.github.wimdeblauwe.shadleaf.pagination;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.paging.PagingParameters;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

class PaginationComponentTest {

  private static final ComponentRenderTester TESTER = ComponentRenderTester.create();

  /** Page {@code number} (zero-based) of {@code total} rows, {@code size} a page, sorted by name. */
  private static Page<String> page(int number, int size, long total) {
    int rows = (int) Math.max(0, Math.min(size, total - (long) number * size));
    return new PageImpl<>(Collections.nCopies(rows, "row"), PageRequest.of(number, size, Sort.by("name")), total);
  }

  private static Rendered pagination(String requestUri, Object page) {
    return ComponentRenderTester.builder().requestUri(requestUri).build()
        .render("<sl:pagination th:page=\"${people}\"/>", Map.of("people", page));
  }

  /** The text of every entry between Previous and Next: page numbers, and "…" for an ellipsis. */
  private static List<String> entries(Rendered rendered) {
    List<Element> items = rendered.select(".pagination-item");
    return items.subList(1, items.size() - 1).stream()
        .map(item -> item.selectFirst(".pagination-ellipsis") != null ? "…" : item.text())
        .toList();
  }

  @Test
  void aPageGetsFirstLastAndTheCurrentPageWithItsSiblings() {
    Rendered rendered = pagination("/people?q=ada&sort=name,asc&page=4&size=10", page(4, 10, 270));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("nav").hasClass("pagination").hasAttribute("aria-label", "Pagination");
    assertThat(rendered).element("nav > ul").hasClass("pagination-content");
    Assertions.assertThat(entries(rendered)).containsExactly("1", "…", "4", "5", "6", "…", "27");
    assertThat(rendered).element("#page-previous")
        .hasClass("btn", "pagination-link", "pagination-previous")
        .hasAttribute("data-variant", "ghost")
        .hasNoAttribute("data-size", "aria-disabled", "role", "tabindex")
        .hasAttribute("aria-label", "Go to previous page")
        .hasAttribute("href", "/people?q=ada&sort=name,asc&size=10&page=3")
        .hasText("Previous");
    assertThat(rendered).element("#page-next")
        .hasAttribute("aria-label", "Go to next page")
        .hasAttribute("href", "/people?q=ada&sort=name,asc&size=10&page=5")
        .hasText("Next");
    assertThat(rendered).element("#page-5")
        .hasClass("btn", "pagination-link")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("data-size", "icon")
        .hasAttribute("aria-current", "page")
        .hasAttribute("href", "/people?q=ada&sort=name,asc&size=10&page=4");
    assertThat(rendered).element("#page-4")
        .hasAttribute("data-variant", "ghost")
        .hasNoAttribute("aria-current");
    assertThat(rendered).element("#page-1").hasAttribute("href", "/people?q=ada&sort=name,asc&size=10&page=0");
    assertThat(rendered).element("#page-27").hasAttribute("href", "/people?q=ada&sort=name,asc&size=10&page=26");
    assertThat(rendered).element(".pagination-ellipsis").hasNoAttribute("aria-hidden").hasText("More pages");
    assertThat(rendered).element(".pagination-ellipsis > .sl-sr-only").hasText("More pages");
  }

  @Test
  void aGapOfOnePageShowsThatPage() {
    Assertions.assertThat(entries(pagination("/people", page(2, 10, 270))))
        .containsExactly("1", "2", "3", "4", "…", "27");
    Assertions.assertThat(entries(pagination("/people", page(3, 10, 70))))
        .containsExactly("1", "2", "3", "4", "5", "6", "7");
    Assertions.assertThat(entries(pagination("/people", page(0, 10, 30)))).containsExactly("1", "2", "3");
  }

  @Test
  void siblingsWidenTheWindow() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people").build()
        .render("<sl:pagination th:page=\"${people}\" siblings=\"2\"/>", Map.of("people", page(9, 10, 270)));
    Assertions.assertThat(entries(rendered)).containsExactly("1", "…", "8", "9", "10", "11", "12", "…", "27");

    Rendered none = ComponentRenderTester.builder().requestUri("/people").build()
        .render("<sl:pagination th:page=\"${people}\" siblings=\"0\"/>", Map.of("people", page(9, 10, 270)));
    Assertions.assertThat(entries(none)).containsExactly("1", "…", "10", "…", "27");
  }

  @Test
  void previousIsDisabledOnTheFirstPageAndNextOnTheLast() {
    Rendered first = pagination("/people", page(0, 10, 270));

    assertThat(first).element("#page-previous")
        .hasNoAttribute("href")
        .hasAttribute("aria-disabled", "true")
        .hasAttribute("role", "link")
        .hasAttribute("tabindex", "-1");
    assertThat(first).element("#page-next").hasAttribute("href", "/people?page=1").hasNoAttribute("aria-disabled");

    Rendered last = pagination("/people", page(26, 10, 270));
    assertThat(last).element("#page-previous").hasAttribute("href", "/people?page=25");
    assertThat(last).element("#page-next").hasNoAttribute("href").hasAttribute("aria-disabled", "true");
  }

  @Test
  void anEmptyResultHasOnePageAndNowhereToGo() {
    Rendered rendered = pagination("/people?q=nobody", page(0, 10, 0));

    Assertions.assertThat(entries(rendered)).containsExactly("1");
    assertThat(rendered).element("#page-1").hasAttribute("aria-current", "page");
    assertThat(rendered).element("#page-previous").hasNoAttribute("href");
    assertThat(rendered).element("#page-next").hasNoAttribute("href");
  }

  @Test
  void pastTheLastPagePreviousLeadsBackToIt() {
    Rendered rendered = pagination("/people?page=98", page(98, 10, 270));

    assertThat(rendered).element("#page-previous").hasAttribute("href", "/people?page=26");
    assertThat(rendered).element("#page-next").hasNoAttribute("href");
    assertThat(rendered).hasNoElement("[aria-current]");
  }

  @Test
  void aSliceGetsPreviousAndNextOnly() {
    SliceImpl<String> slice = new SliceImpl<>(List.of("row"), PageRequest.of(1, 10), true);
    Rendered rendered = pagination("/people?page=1", slice);

    Assertions.assertThat(rendered.select(".pagination-item")).hasSize(2);
    assertThat(rendered).element("#page-previous").hasAttribute("href", "/people?page=0");
    assertThat(rendered).element("#page-next").hasAttribute("href", "/people?page=2");

    SliceImpl<String> lastSlice = new SliceImpl<>(List.of("row"), PageRequest.of(1, 10), false);
    assertThat(pagination("/people?page=1", lastSlice)).element("#page-next").hasNoAttribute("href");
  }

  @Test
  void withoutSpringDataCurrentAndTotalAreEnough() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/search?q=x").build()
        .render("<sl:pagination current=\"2\" total=\"3\"/>");

    Assertions.assertThat(entries(rendered)).containsExactly("1", "2", "3");
    assertThat(rendered).element("#page-2").hasAttribute("aria-current", "page");
    assertThat(rendered).element("#page-previous").hasAttribute("href", "/search?q=x&page=0");
    assertThat(rendered).element("#page-next").hasAttribute("href", "/search?q=x&page=2");

    Rendered last = ComponentRenderTester.builder().requestUri("/search").build()
        .render("<sl:pagination th:current=\"${3}\" th:total=\"${3}\"/>");
    assertThat(last).element("#page-next").hasNoAttribute("href");
  }

  @Test
  void oneIndexedParametersCountFromOne() {
    Rendered rendered = ComponentRenderTester.builder()
        .pagingParameters(new PagingParameters("page", "size", true, "", "_", "sort"))
        .requestUri("/people?page=2")
        .build()
        .render("<sl:pagination th:page=\"${people}\"/>", Map.of("people", page(1, 10, 30)));

    assertThat(rendered).element("#page-previous").hasAttribute("href", "/people?page=1");
    assertThat(rendered).element("#page-3").hasAttribute("href", "/people?page=3");
  }

  @Test
  void aQualifierGivesThePaginationItsOwnParametersAndIds() {
    Rendered rendered = ComponentRenderTester.builder()
        .requestUri("/team?page=3&members_page=1&members_size=5")
        .build()
        .render("<sl:pagination th:page=\"${members}\" qualifier=\"members\"/>",
            Map.of("members", page(1, 5, 15)));

    assertThat(rendered).element("#members-page-previous").hasAttribute("href", "/team?page=3&members_size=5&members_page=0");
    assertThat(rendered).element("#members-page-3").hasAttribute("href", "/team?page=3&members_size=5&members_page=2");
    assertThat(rendered).hasNoElement("#page-1");
  }

  @Test
  void linksStartWithTheContextPath() {
    Rendered rendered = ComponentRenderTester.builder().contextPath("/shop").requestUri("/people").build()
        .render("<sl:pagination th:page=\"${people}\"/>", Map.of("people", page(0, 10, 20)));

    assertThat(rendered).element("#page-2").hasAttribute("href", "/shop/people?page=1");
  }

  @Test
  void anAriaLabelReplacesTheNavsName() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people").build()
        .render("<sl:pagination th:page=\"${people}\" aria-label=\"People pages\" class=\"extra\"/>",
            Map.of("people", page(0, 10, 20)));

    assertThat(rendered).root().hasAttribute("aria-label", "People pages").hasClass("pagination", "extra");
  }

  @Test
  void theLabelsAreMessages() {
    Rendered rendered = ComponentRenderTester.builder().locale(Locale.ENGLISH).requestUri("/people").build()
        .render("<sl:pagination th:page=\"${people}\"/>", Map.of("people", page(0, 10, 20)));

    assertThat(rendered).element(".pagination-previous-text").hasText("Previous");
    assertThat(rendered).element(".pagination-next-text").hasText("Next");
  }

  @Test
  void contentOfItsOwnIsAHandWrittenPagination() {
    Rendered rendered = TESTER.render("""
        <sl:pagination>
          <sl:pagination-content>
            <sl:pagination-item><sl:pagination-previous href="?page=1"/></sl:pagination-item>
            <sl:pagination-item><sl:pagination-link href="?page=1">1</sl:pagination-link></sl:pagination-item>
            <sl:pagination-item><sl:pagination-link href="?page=2" active>2</sl:pagination-link></sl:pagination-item>
            <sl:pagination-item><sl:pagination-ellipsis/></sl:pagination-item>
            <sl:pagination-item><sl:pagination-next disabled href="?page=3">Newer</sl:pagination-next></sl:pagination-item>
          </sl:pagination-content>
        </sl:pagination>""");

    assertThat(rendered).hasNoLeakedMarkup();
    Assertions.assertThat(rendered.select("li")).hasSize(5);
    assertThat(rendered).element("a[aria-current]").hasAttribute("href", "?page=2").hasAttribute("data-variant",
        "outline");
    assertThat(rendered).element(".pagination-next")
        .hasNoAttribute("href")
        .hasAttribute("aria-disabled", "true")
        .hasText("Newer");
  }

  @Test
  void aLinkTakesAButtonSize() {
    Rendered rendered = TESTER.render("<sl:pagination-link href=\"?page=1\" size=\"default\">First</sl:pagination-link>");

    assertThat(rendered).root().hasNoAttribute("data-size").hasAttribute("data-variant", "ghost");
  }

  @Test
  void withoutAPageOrNumbersItFails() {
    assertThatRenderFailure(() -> TESTER.render("<sl:pagination/>"))
        .hasMessageContaining("sl:pagination needs the page shown now");
    assertThatRenderFailure(() -> TESTER.render("<sl:pagination th:page=\"${people}\"/>",
        Map.of("people", List.of("Ada"))))
        .hasMessageContaining("th:page takes a Spring Data Page or Slice");
  }
}
