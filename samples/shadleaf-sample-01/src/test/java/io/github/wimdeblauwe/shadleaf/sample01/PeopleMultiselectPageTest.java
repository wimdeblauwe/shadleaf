package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import java.net.URI;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The people page with multiselect: the people table with a checkbox per row, select-all and a count, in a form that
 * deletes the selected people after a confirmation, with and without htmx, and comes back to the same search, order
 * and page. The tests that delete reset the database afterwards (a new context).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PeopleMultiselectPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PersonRepository repository;

  private Document page(String url) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(URI.create(url)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }

  /** The texts of a column of data, counted from 1 after the selection column. */
  private static List<String> column(Document page, int column) {
    return page.select(".table > tbody > tr > td:nth-child(" + (column + 1) + ")").eachText();
  }

  private static List<String> ids(Document page) {
    return page.select(".table > tbody input[name=ids]").eachAttr("value");
  }

  @Test
  void theSearchFormAndTheLinksStayOnThisPage() throws Exception {
    Document page = page("/people-multiselect?q=ada");

    assertThat(page.select("h1").text()).isEqualTo("People with multiselect");
    assertThat(page.getElementById("people-search").attr("action")).isEqualTo("/people-multiselect");
    assertThat(page.getElementById("people-search").attr("hx-get")).isEqualTo("/people-multiselect");
    assertThat(page.getElementById("sort-email").attr("href")).isEqualTo("/people-multiselect?q=ada&sort=email,asc");
    assertThat(column(page, 1)).isNotEmpty().allMatch(name -> name.toLowerCase().contains("ada"));
    assertThat(page.select("[id]").eachAttr("id")).doesNotHaveDuplicates();
  }

  @Test
  void anHtmxRequestGetsTheResultsAlone() throws Exception {
    Document fragment = Jsoup.parseBodyFragment(mockMvc.perform(get("/people-multiselect?q=ada")
            .header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    assertThat(fragment.select("h1, #people-search")).isEmpty();
    assertThat(fragment.body().child(0).id()).isEqualTo("people-results");
    assertThat(fragment.select("#people-table .table-select-all")).hasSize(1);
  }

  @Test
  void everyRowHasACheckboxNamedAfterItsPersonInAFormThatPostsToThisPage() throws Exception {
    Document page = page("/people-multiselect?q=ada&sort=email,asc&size=10");

    Element form = page.getElementById("people-bulk");
    assertThat(form.attr("method")).isEqualTo("post");
    assertThat(form.attr("action")).isEqualTo("/people-multiselect?q=ada&sort=email,asc&size=10");
    List<String> names = column(page, 1);
    assertThat(form.select(".table > tbody input[type=checkbox][name=ids]").eachAttr("aria-label"))
        .isNotEmpty()
        .isEqualTo(names.stream().map(name -> "Select " + name).toList());
    assertThat(ids(page)).allMatch(id -> id.matches("\\d+"));
    Element selectAll = page.selectFirst("#people-table > thead .table-select-all");
    assertThat(selectAll.attr("x-data")).isEqualTo("slTableSelection");
    assertThat(selectAll.selectFirst("input").attr("aria-label")).isEqualTo("Select all rows on this page");
    assertThat(selectAll.selectFirst("input").hasAttr("name")).isFalse();
    assertThat(page.select(".table-footer .table-selection-count").attr("data-table")).isEqualTo("people-table");
    Element confirm = page.selectFirst("#people-delete button[type=submit]");
    assertThat(confirm.attr("hx-post")).isEqualTo(form.attr("action"));
    assertThat(confirm.closest("form")).isEqualTo(form);
    assertThat(page.getElementById("people-delete-trigger").attr("commandfor")).isEqualTo("people-delete");
  }

  @Test
  @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
  void deletingWithoutHtmxRedirectsBackToTheSamePageWithAToast() throws Exception {
    String url = "/people-multiselect?q=a&sort=orders,desc&page=1";
    List<String> selected = ids(page(url)).subList(0, 2);

    MvcResult result = mockMvc.perform(post(URI.create(url)).param("ids", selected.toArray(String[]::new)))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl(url))
        .andExpect(flash().attribute("toasts", List.of(Toast.success("2 people deleted"))))
        .andReturn();

    assertThat(repository.count()).isEqualTo(268);
    selected.forEach(id -> assertThat(repository.existsById(Long.valueOf(id))).isFalse());
    Document after = Jsoup.parse(mockMvc.perform(get(URI.create(url)).flashAttrs(result.getFlashMap()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    assertThat(after.select(".toaster-list > li .toast-title").eachText()).containsExactly("2 people deleted");
    assertThat(ids(after)).doesNotContainAnyElementsOf(selected);
    assertThat(after.select("th[aria-sort]").eachAttr("aria-sort")).containsExactly("descending");
  }

  @Test
  @DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
  void deletingWithHtmxAnswersWithTheResultsOfTheSamePage() throws Exception {
    String url = "/people-multiselect?q=a&sort=orders,desc&page=1";
    String selected = ids(page(url)).get(0);

    MvcResult result = mockMvc.perform(post(URI.create(url)).param("ids", selected).header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andReturn();

    String trigger = result.getResponse().getHeader("HX-Trigger");
    assertThat(trigger).contains("sl-dialog-close", "sl-toast", "1 person deleted");
    assertThat(trigger.indexOf("sl-dialog-close")).isLessThan(trigger.indexOf("sl-toast"));
    Document fragment = Jsoup.parseBodyFragment(result.getResponse().getContentAsString());
    assertThat(fragment.body().child(0).id()).isEqualTo("people-results");
    assertThat(ids(fragment)).doesNotContain(selected).hasSize(PeopleController.PAGE_SIZE);
    assertThat(fragment.getElementById("people-bulk").attr("action")).isEqualTo(url);
    assertThat(fragment.getElementById("sort-orders").attr("href")).isEqualTo("/people-multiselect?q=a&sort=orders,asc");
    assertThat(fragment.getElementById("people-delete-trigger")).isNotNull();
    assertThat(repository.existsById(Long.valueOf(selected))).isFalse();
  }

  @Test
  void deletingNothingSaysSo() throws Exception {
    mockMvc.perform(post("/people-multiselect"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/people-multiselect"))
        .andExpect(flash().attribute("toasts",
            List.of(Toast.warning("Nothing was deleted").withDescription("Select the people to delete first."))));

    assertThat(repository.count()).isEqualTo(270);
  }
}
