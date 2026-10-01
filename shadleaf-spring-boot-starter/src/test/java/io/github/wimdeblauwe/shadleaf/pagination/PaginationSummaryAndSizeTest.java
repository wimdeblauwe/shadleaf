package io.github.wimdeblauwe.shadleaf.pagination;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

class PaginationSummaryAndSizeTest {

  private static final ComponentRenderTester TESTER = ComponentRenderTester.create();

  private static Page<String> page(int number, int size, long total) {
    int rows = (int) Math.max(0, Math.min(size, total - (long) number * size));
    return new PageImpl<>(Collections.nCopies(rows, "row"), PageRequest.of(number, size), total);
  }

  private static String summary(String snippet, Object page) {
    return TESTER.render(snippet, Map.of("people", page)).root().text();
  }

  @Test
  void theSummaryShowsTheRowsAndTheTotal() {
    Rendered rendered = TESTER.render("<sl:pagination-summary th:page=\"${people}\" class=\"extra\"/>",
        Map.of("people", page(1, 10, 270)));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClass("pagination-summary", "extra").hasText("11–20 of 270");
  }

  @Test
  void theSummaryOfTheLastPageEndsAtTheTotal() {
    Assertions.assertThat(summary("<sl:pagination-summary th:page=\"${people}\"/>", page(26, 10, 265)))
        .isEqualTo("261–265 of 265");
    Assertions.assertThat(summary("<sl:pagination-summary th:page=\"${people}\"/>", page(0, 10, 0)))
        .isEqualTo("0 of 0");
  }

  @Test
  void theSummaryCanShowThePage() {
    String snippet = "<sl:pagination-summary th:page=\"${people}\" format=\"page\"/>";

    Assertions.assertThat(summary(snippet, page(1, 10, 270))).isEqualTo("Page 2 of 27");
    Assertions.assertThat(summary(snippet, page(0, 10, 0))).isEqualTo("Page 1 of 1");
  }

  @Test
  void aSliceHasNoTotal() {
    SliceImpl<String> slice = new SliceImpl<>(Collections.nCopies(10, "row"), PageRequest.of(1, 10), true);

    Assertions.assertThat(summary("<sl:pagination-summary th:page=\"${people}\"/>", slice)).isEqualTo("11–20");
    Assertions.assertThat(summary("<sl:pagination-summary th:page=\"${people}\" format=\"page\"/>", slice))
        .isEqualTo("Page 2");
  }

  @Test
  void numbersAreFormattedForTheLocale() {
    Assertions.assertThat(ComponentRenderTester.builder().locale(Locale.ENGLISH).build()
            .render("<sl:pagination-summary th:page=\"${people}\"/>", Map.of("people", page(100, 10, 12345)))
            .root().text())
        .isEqualTo("1,001–1,010 of 12,345");
  }

  @Test
  void theApplicationRewordsTheSummary() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.pagination.summary", Locale.ENGLISH, "Showing {0} to {1} of {2} people");
    Rendered rendered = ComponentRenderTester.builder().messageSource(messages).locale(Locale.ENGLISH).build()
        .render("<sl:pagination-summary th:page=\"${people}\"/>", Map.of("people", page(0, 10, 25)));

    assertThat(rendered).root().hasText("Showing 1 to 10 of 25 people");
  }

  @Test
  void theSizeMenuOffersTheSizesAsLinks() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people?q=ada&page=3&sort=name,asc&size=20")
        .build()
        .render("<sl:pagination-size th:page=\"${people}\" class=\"extra\"/>", Map.of("people", page(3, 20, 270)));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClass("pagination-size", "extra");
    assertThat(rendered).element("#page-size-label").hasClass("pagination-size-label").hasText("Rows per page");
    assertThat(rendered).element("#page-size-trigger")
        .hasClass("btn", "dropdown-menu-trigger", "pagination-size-trigger")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("data-size", "sm")
        .hasAttribute("popovertarget", "page-size")
        .hasAttribute("aria-labelledby", "page-size-label page-size-trigger")
        .hasText("20");
    assertThat(rendered).element("#page-size")
        .hasClass("dropdown-menu-content", "pagination-size-content")
        .hasAttribute("data-side", "top")
        .hasAttribute("aria-labelledby", "page-size-label");
    List<Element> items = rendered.select("#page-size a[role=menuitemradio]");
    Assertions.assertThat(items).extracting(Element::text).containsExactly("10", "20", "50");
    Assertions.assertThat(items).extracting(item -> item.attr("href")).containsExactly(
        "/people?q=ada&sort=name,asc&size=10",
        "/people?q=ada&sort=name,asc&size=20",
        "/people?q=ada&sort=name,asc&size=50");
    Assertions.assertThat(items).extracting(item -> item.attr("aria-checked"))
        .containsExactly("false", "true", "false");
  }

  @Test
  void aSizeThatIsNotOfferedIsAdded() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people?size=25").build()
        .render("<sl:pagination-size th:page=\"${people}\" sizes=\"10, 50 100\"/>",
            Map.of("people", page(0, 25, 270)));

    Assertions.assertThat(rendered.select("[role=menuitemradio]")).extracting(Element::text)
        .containsExactly("10", "25", "50", "100");
    assertThat(rendered).element("[aria-checked=true]").hasText("25");
  }

  @Test
  void aQualifierGivesTheSizeMenuItsOwnParameterAndIds() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/team?members_page=2&size=50").build()
        .render("<sl:pagination-size th:page=\"${members}\" qualifier=\"members\"/>",
            Map.of("members", page(2, 10, 50)));

    assertThat(rendered).element("#members-page-size-trigger")
        .hasAttribute("aria-labelledby", "members-page-size-label members-page-size-trigger");
    assertThat(rendered).element("[aria-checked=true]").hasAttribute("href", "/team?size=50&members_size=10");
  }

  @Test
  void anIdReplacesTheMenusId() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people").build()
        .render("<sl:pagination-size th:page=\"${people}\" id=\"rows\"/>", Map.of("people", page(0, 10, 20)));

    assertThat(rendered).element("#rows-trigger").hasAttribute("aria-labelledby", "rows-label rows-trigger");
    assertThat(rendered).root().hasNoAttribute("id");
  }

  @Test
  void bothNeedAPage() {
    assertThatRenderFailure(() -> TESTER.render("<sl:pagination-summary/>"))
        .hasMessageContaining("sl:pagination-summary needs the page shown now");
    assertThatRenderFailure(() -> TESTER.render("<sl:pagination-size/>"))
        .hasMessageContaining("sl:pagination-size needs the page shown now");
    assertThatRenderFailure(() -> TESTER.render("<sl:pagination-size th:page=\"${people}\" sizes=\"10,many\"/>",
        Map.of("people", page(0, 10, 20))))
        .hasMessageContaining("'many' in sizes=\"10,many\" is no number");
  }
}
