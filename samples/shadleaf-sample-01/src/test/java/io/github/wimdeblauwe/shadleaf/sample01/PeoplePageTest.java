package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The people page: a table sorted by Spring Data JPA from the {@code sort} parameter its column headers link to.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PeoplePageTest {

  @Autowired
  private MockMvc mockMvc;

  private Document page(String url) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(url))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
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
}
