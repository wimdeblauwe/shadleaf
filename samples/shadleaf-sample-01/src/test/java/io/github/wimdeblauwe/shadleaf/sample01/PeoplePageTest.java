package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The people page: a table sorted and paged by Spring Data JPA from the parameters its column headers, pagination
 * and size menu link to, filtered by the search form above it; with htmx, only the table and its footer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PeoplePageTest {

  @Autowired
  private MockMvc mockMvc;

  private Document page(String url) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(URI.create(url)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }

  private String html(String url, String... headers) throws Exception {
    MockHttpServletRequestBuilder request = get(URI.create(url));
    for (int i = 0; i < headers.length; i += 2) {
      request.header(headers[i], headers[i + 1]);
    }
    return mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
  }

  private static List<String> column(Document page, int column) {
    return page.select(".table > tbody > tr > td:nth-child(" + column + ")").eachText();
  }

  @Test
  void sortedByNameByDefault() throws Exception {
    Document page = page("/people");

    assertThat(column(page, 1)).hasSize(PeopleController.PAGE_SIZE).isSorted();
    assertThat(page.select("th[aria-sort]").eachAttr("aria-sort")).containsExactly("ascending");
    assertThat(page.select("th[aria-sort] > a").attr("id")).isEqualTo("sort-name");
    assertThat(page.getElementById("sort-name").attr("href")).isEqualTo("/people?sort=name,desc");
    assertThat(page.getElementById("sort-orders").attr("href")).isEqualTo("/people?sort=orders,asc");
    assertThat(page.select("[id]").eachAttr("id")).doesNotHaveDuplicates();
  }

  @Test
  void theHeadersLinkSortsTheRows() throws Exception {
    Document page = page("/people?sort=orders,desc");

    List<Integer> orders = column(page, 4).stream().map(Integer::valueOf).toList();
    assertThat(orders).isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(page.select("th[aria-sort]").eachAttr("aria-sort")).containsExactly("descending");
    assertThat(page.getElementById("sort-orders").attr("href")).isEqualTo("/people?sort=orders,asc");
  }

  @Test
  void aPropertyTheTableDoesNotOfferFallsBackToName() throws Exception {
    Document page = page("/people?sort=id,desc");

    assertThat(column(page, 1)).isSorted();
    assertThat(page.select("th[aria-sort] > a").attr("id")).isEqualTo("sort-name");
  }

  @Test
  void theFooterShowsWhereThePageIs() throws Exception {
    Document page = page("/people?sort=orders,desc&page=1");

    assertThat(page.select(".table-footer .pagination-summary").text()).isEqualTo("21–40 of 270");
    assertThat(page.select(".pagination [aria-current=page]").text()).isEqualTo("2");
    assertThat(page.getElementById("page-previous").attr("href")).isEqualTo("/people?sort=orders,desc&page=0");
    assertThat(page.getElementById("page-next").attr("href")).isEqualTo("/people?sort=orders,desc&page=2");
    assertThat(page.getElementById("page-14").attr("href")).isEqualTo("/people?sort=orders,desc&page=13");
    assertThat(page.getElementById("sort-orders").attr("href")).isEqualTo("/people?sort=orders,asc");
    assertThat(page.select("[id]").eachAttr("id")).doesNotHaveDuplicates();
  }

  @Test
  void theSizeMenuChangesTheRowsPerPage() throws Exception {
    Document page = page("/people?sort=name,asc&page=3&size=50");

    assertThat(column(page, 1)).hasSize(50);
    assertThat(page.select(".pagination-size-trigger").text()).isEqualTo("50");
    assertThat(page.select("#page-size [aria-checked=true]").attr("href")).isEqualTo("/people?sort=name,asc&size=50");
    assertThat(page.select("#page-size [role=menuitemradio]").eachAttr("href")).containsExactly(
        "/people?sort=name,asc&size=10", "/people?sort=name,asc&size=20", "/people?sort=name,asc&size=50");
    assertThat(page.select(".pagination-summary").text()).isEqualTo("151–200 of 270");
  }

  /**
   * Many people share a number of orders, a role or a join year: following Next from the first page to the last must
   * still show every person exactly once, which takes the id as the controller's last sort key.
   */
  @ParameterizedTest
  @ValueSource(strings = {"/people?sort=orders,asc&size=20", "/people?sort=orders,desc&size=20",
      "/people?sort=role,asc&size=10", "/people?sort=role,desc&size=50", "/people"})
  void everyPersonIsOnExactlyOnePage(String firstPage) throws Exception {
    List<String> emails = new ArrayList<>();
    String url = firstPage;
    int pages = 0;
    while (url != null) {
      Document page = page(url);
      emails.addAll(column(page, 2));
      Element next = page.getElementById("page-next");
      url = next.hasAttr("href") ? next.attr("href") : null;
      pages++;
      assertThat(pages).as("pages followed from %s", firstPage).isLessThanOrEqualTo(270);
    }

    assertThat(emails).hasSize(270).doesNotHaveDuplicates();
  }

  @Test
  void rolesSortByRankNotByName() throws Exception {
    Document page = page("/people?sort=role,asc&size=50");

    List<String> roles = column(page, 3);
    List<String> ranked = List.of("Owner", "Admin", "Member", "Guest");
    assertThat(roles).isSortedAccordingTo(Comparator.comparing(ranked::indexOf));
    assertThat(roles.get(0)).isEqualTo("Owner");
    assertThat(page.select("th[aria-sort]").eachAttr("aria-sort")).containsExactly("ascending");

    List<String> descending = column(page("/people?sort=role,desc&size=50"), 3);
    assertThat(descending.get(0)).isEqualTo("Guest");
  }

  @Test
  void theSearchFiltersOnNameAndEmail() throws Exception {
    Document page = page("/people?q=ADA&size=50");

    List<String> names = column(page, 1);
    List<String> emails = column(page, 2);
    assertThat(names).isNotEmpty().isSorted();
    for (int row = 0; row < names.size(); row++) {
      assertThat(names.get(row) + " " + emails.get(row)).containsIgnoringCase("ada");
    }
    assertThat(page.getElementById("people-q").attr("value")).isEqualTo("ADA");
    assertThat(page.getElementById("people-status").text()).isEqualTo(names.size() + " people found");
    assertThat(page.select(".pagination-summary").text()).isEqualTo("1–" + names.size() + " of " + names.size());
  }

  @Test
  void sortPagesAndSizesKeepTheSearch() throws Exception {
    Document page = page("/people?q=e&sort=orders,desc&page=1");

    assertThat(page.getElementById("sort-name").attr("href")).isEqualTo("/people?q=e&sort=name,asc");
    assertThat(page.getElementById("sort-orders").attr("href")).isEqualTo("/people?q=e&sort=orders,asc");
    assertThat(page.getElementById("page-next").attr("href")).isEqualTo("/people?q=e&sort=orders,desc&page=2");
    assertThat(page.select("#page-size [role=menuitemradio]").eachAttr("href"))
        .contains("/people?q=e&sort=orders,desc&size=50");
  }

  /** The hidden inputs carry the order and the size, not the search (the field has it) and not the page. */
  @Test
  void aSearchKeepsTheOrderAndSizeAndStartsOnTheFirstPage() throws Exception {
    Document page = page("/people?q=ada&sort=orders,desc&size=50&page=1");

    Element form = page.getElementById("people-search");
    assertThat(form.attr("method")).isEqualTo("get");
    assertThat(form.attr("action")).isEqualTo("/people");
    List<String> hidden = page.select("input[type=hidden][form=people-search]").stream()
        .map(input -> input.attr("name") + "=" + input.attr("value"))
        .toList();
    assertThat(hidden).containsExactly("sort=orders,desc", "size=50");
    assertThat(form.select("input[name=q]")).hasSize(1);
  }

  @Test
  void nothingFoundSaysWhatWasSearched() throws Exception {
    Document page = page("/people?q=zzz%3Cb%3E");

    assertThat(page.select(".table > tbody > tr")).hasSize(1);
    assertThat(page.select(".table-empty").text()).isEqualTo("No one matches “zzz<b>”.");
    assertThat(page.select(".table-empty b")).isEmpty();
    assertThat(page.select(".pagination-summary").text()).isEqualTo("0 of 0");
    assertThat(page.getElementById("people-status").text()).isEqualTo("0 people found");
  }

  @Test
  void anHtmxRequestGetsTheResultsAlone() throws Exception {
    mockMvc.perform(get("/people?q=ada&sort=email,asc").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andExpect(header().stringValues("Vary", org.hamcrest.Matchers.hasItem("HX-Request")));
    Document fragment = Jsoup.parseBodyFragment(html("/people?q=ada&sort=email,asc", "HX-Request", "true"));

    assertThat(fragment.select("h1, nav.site-nav, #people-search")).isEmpty();
    Element results = fragment.body().child(0);
    assertThat(results.id()).isEqualTo("people-results");
    assertThat(results.select(".table > tbody > tr")).isNotEmpty();
    assertThat(column(fragment, 2)).isSorted();
    assertThat(fragment.getElementById("sort-email").attr("href")).isEqualTo("/people?q=ada&sort=email,desc");
    assertThat(fragment.select("#people-status[hx-swap-oob=innerHTML]").text()).endsWith("people found");
    assertThat(fragment.select("input[type=hidden][form=people-search]").eachAttr("name")).containsExactly("sort");
  }

  /** Back to an entry htmx did not keep (hx-history="false"): htmx asks for the whole page. */
  @Test
  void aHistoryRestoreGetsTheWholePage() throws Exception {
    Document page = Jsoup.parse(html("/people?q=ada", "HX-Request", "true", "HX-History-Restore-Request", "true"));

    assertThat(page.select("h1").text()).isEqualTo("People");
    assertThat(page.getElementById("people-q").attr("value")).isEqualTo("ada");
    assertThat(page.select("#people-status[hx-swap-oob]")).isEmpty();
    assertThat(page.select("[id]").eachAttr("id")).doesNotHaveDuplicates();
  }

  @Test
  void thePageWithoutHtmxHasEveryIdOnce() throws Exception {
    Document page = page("/people?q=a&sort=role,desc&size=10&page=2");

    assertThat(page.select("[id]").eachAttr("id")).doesNotHaveDuplicates();
    assertThat(page.select("#people-status[hx-swap-oob]")).isEmpty();
  }
}
