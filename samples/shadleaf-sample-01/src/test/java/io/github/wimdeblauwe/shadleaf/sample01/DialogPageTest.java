package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The dialog page: a dialog on the page, and an edit dialog htmx fetches, whose form swaps itself while it has errors
 * and closes the dialog with {@code HX-Trigger: sl-dialog-close} after a valid save.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class DialogPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theButtonOpensTheDialogOnThePage() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/dialog"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    Element trigger = page.selectFirst("button[commandfor=shortcuts]");
    assertThat(trigger.attr("command")).isEqualTo("show-modal");
    Element dialog = page.getElementById("shortcuts");
    assertThat(dialog.tagName()).isEqualTo("dialog");
    assertThat(dialog.attr("aria-labelledby")).isEqualTo("shortcuts-title");
    assertThat(dialog.hasAttr("data-show-modal")).isFalse();
    assertThat(page.select("#modal-root")).hasSize(1);
    assertThat(page.select("tbody tr")).hasSize(3);
    assertThat(page.selectFirst("#member-1 button").attr("hx-get")).isEqualTo("/dialog/members/1/edit");
  }

  @Test
  void editFetchesADialogThatOpensItself() throws Exception {
    Document fragment = fragment(mockMvc.perform(htmx(get("/dialog/members/2/edit")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    Element dialog = fragment.body().child(0);
    assertThat(dialog.id()).isEqualTo("edit-member");
    assertThat(dialog.attr("data-show-modal")).isEqualTo("true");
    assertThat(dialog.getElementById("name").val()).isEqualTo("Grace Hopper");
    Element form = dialog.getElementById("edit-member-form");
    assertThat(form.attr("hx-post")).isEqualTo("/dialog/members/2");
    assertThat(form.attr("hx-target")).isEqualTo("this");
    assertThat(dialog.selectFirst("button[type=submit]").attr("form")).isEqualTo("edit-member-form");
  }

  @Test
  void aSaveWithErrorsReturnsTheFormAloneWithTheFocusOnTheFirstFieldWithAnError() throws Exception {
    String html = mockMvc.perform(htmx(post("/dialog/members/3").param("name", "Alan").param("email", "nope")))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("HX-Trigger"))
        .andReturn().getResponse().getContentAsString();

    Document fragment = fragment(html);
    assertThat(fragment.body().children()).extracting(Element::id).containsExactly("edit-member-form");
    assertThat(fragment.select("[autofocus]")).extracting(Element::id).containsExactly("email");
    assertThat(fragment.getElementById("email").attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void aValidSaveClosesTheDialogAndUpdatesTheRowOutOfBand() throws Exception {
    String html = mockMvc.perform(htmx(post("/dialog/members/1")
            .param("name", "Ada King").param("email", "ada.king@example.com")))
        .andExpect(status().isOk())
        .andExpect(header().string("HX-Trigger", "sl-dialog-close"))
        .andExpect(header().string("HX-Reswap", "none"))
        .andReturn().getResponse().getContentAsString();

    Element row = Jsoup.parseBodyFragment("<table>" + html + "</table>").getElementById("member-1");
    assertThat(row.attr("hx-swap-oob")).isEqualTo("true");
    assertThat(row.child(0).text()).isEqualTo("Ada King");

    Document page = Jsoup.parse(mockMvc.perform(get("/dialog")).andReturn().getResponse().getContentAsString());
    assertThat(page.getElementById("member-1").child(0).text()).isEqualTo("Ada King");
    assertThat(page.getElementById("member-1").hasAttr("hx-swap-oob")).isFalse();
  }

  private static Document fragment(String html) {
    assertThat(html).doesNotContain("<html", "<head", "page-header");
    return Jsoup.parseBodyFragment(html);
  }

  private static MockHttpServletRequestBuilder htmx(MockHttpServletRequestBuilder request) {
    return request.header("HX-Request", "true");
  }
}
