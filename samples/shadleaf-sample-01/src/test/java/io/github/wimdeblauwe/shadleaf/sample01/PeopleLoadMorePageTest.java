package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The load more page: the people a {@code Slice} at a time. With htmx a Load more link fetches the next slice's rows
 * and a new Load more row, which replace the old one; without htmx the link opens the page with every slice up to the
 * next one, at the first new row.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PeopleLoadMorePageTest {

  @Autowired
  private MockMvc mockMvc;

  private String html(String url, boolean htmx) throws Exception {
    var request = get(URI.create(url));
    if (htmx) {
      request.header("HX-Request", "true");
    }
    return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  }

  /** The rows of a fragment, parsed in a table body as htmx does. */
  private static List<Element> fragmentRows(String html) {
    return Jsoup.parse("<table><tbody>" + html + "</tbody></table>", "", Parser.htmlParser())
        .select("tbody > tr");
  }

  private static List<String> emails(List<Element> rows) {
    return rows.stream().filter(row -> !row.hasClass("load-more-row")).map(row -> row.child(1).text()).toList();
  }

  @Test
  void theFirstSliceEndsWithALoadMoreLink() throws Exception {
    Document page = Jsoup.parse(html("/people-load-more", false));

    List<Element> rows = page.select(".table > tbody > tr");
    assertThat(rows).hasSize(PeopleController.PAGE_SIZE + 1);
    assertThat(emails(rows)).hasSize(PeopleController.PAGE_SIZE);
    assertThat(rows.get(0).id()).isEqualTo("people-row-1");
    assertThat(page.select("tr[tabindex], [autofocus]")).isEmpty();
    Element link = page.select(".load-more-row a.btn").first();
    assertThat(link.text()).isEqualTo("Load more");
    assertThat(link.attr("href")).isEqualTo("/people-load-more?page=1#people-row-21");
    assertThat(link.attr("hx-get")).isEqualTo("/people-load-more?page=1");
    assertThat(link.attr("hx-target")).isEqualTo("closest tr");
    assertThat(link.attr("hx-swap")).isEqualTo("outerHTML");
    assertThat(link.attr("hx-replace-url")).isEqualTo("true");
    // A Slice has no total: no pagination summary, and the sort links still work.
    assertThat(page.getElementById("sort-email").attr("href")).isEqualTo("/people-load-more?sort=email,asc");
    assertThat(page.select("[id]").eachAttr("id")).doesNotHaveDuplicates();
  }

  @Test
  void withHtmxTheAnswerIsTheNextSlicesRowsWithTheFirstOneFocused() throws Exception {
    List<Element> rows = fragmentRows(html("/people-load-more?sort=orders,desc&page=1", true));

    assertThat(rows).hasSize(PeopleController.PAGE_SIZE + 1);
    Element first = rows.get(0);
    assertThat(first.id()).isEqualTo("people-row-21");
    assertThat(first.attr("tabindex")).isEqualTo("-1");
    assertThat(first.hasAttr("autofocus")).isTrue();
    assertThat(rows.subList(1, rows.size())).noneMatch(row -> row.hasAttr("autofocus") || row.hasAttr("tabindex"));
    assertThat(rows.get(rows.size() - 1).select("a").attr("hx-get")).isEqualTo("/people-load-more?sort=orders,desc&page=2");
    assertThat(rows.get(rows.size() - 1).select("a").attr("href"))
        .isEqualTo("/people-load-more?sort=orders,desc&page=2#people-row-41");
  }

  /** Without htmx (or after a reload) every slice up to the asked one, so the link adds rows as htmx does. */
  @Test
  void withoutHtmxThePageHasEverySliceUpToTheAskedOne() throws Exception {
    Document page = Jsoup.parse(html("/people-load-more?sort=orders,desc&page=2", false));

    List<Element> rows = page.select(".table > tbody > tr");
    assertThat(emails(rows)).hasSize(60).doesNotHaveDuplicates();
    assertThat(page.select("tr[tabindex=-1]").eachAttr("id")).containsExactly("people-row-21", "people-row-41");
    assertThat(page.select("[autofocus]")).isEmpty();
    assertThat(page.select(".load-more-row a").attr("href"))
        .isEqualTo("/people-load-more?sort=orders,desc&page=3#people-row-61");
    List<String> firstSlices = new ArrayList<>(emails(Jsoup.parse(html("/people-load-more?sort=orders,desc&page=1",
        false)).select(".table > tbody > tr")));
    assertThat(emails(rows).subList(0, 40)).isEqualTo(firstSlices);
  }

  /** Following Load more with htmx from the first slice to the last finds everyone once, and the last has no link. */
  @Test
  void loadingEverySliceFindsEveryoneOnce() throws Exception {
    Document page = Jsoup.parse(html("/people-load-more?sort=role,asc", false));
    List<String> emails = new ArrayList<>(emails(page.select(".table > tbody > tr")));
    String next = page.select(".load-more-row a").attr("hx-get");
    int requests = 0;
    while (!next.isEmpty()) {
      List<Element> rows = fragmentRows(html(next, true));
      emails.addAll(emails(rows));
      Element last = rows.get(rows.size() - 1);
      next = last.hasClass("load-more-row") ? last.select("a").attr("hx-get") : "";
      assertThat(++requests).isLessThanOrEqualTo(270);
    }

    assertThat(emails).hasSize(270).doesNotHaveDuplicates();
    assertThat(requests).isEqualTo(13);
  }
}
