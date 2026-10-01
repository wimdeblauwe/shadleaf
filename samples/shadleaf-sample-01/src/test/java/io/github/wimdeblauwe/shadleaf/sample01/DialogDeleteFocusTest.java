package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/**
 * Where the focus goes after a confirmed delete on the dialog page, which removes the row holding the menu button the
 * focus came back to: the next row's menu button in the order the page shows, the previous row's after the last row,
 * and Invite member once the table is empty. The answer re-renders that element out of band with {@code autofocus}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class DialogDeleteFocusTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theFocusGoesToTheNextRowThenThePreviousOneThenInviteMember() throws Exception {
    // Added: Ada (1), Grace (2), Alan (3). By name: Ada, Alan, Grace.
    assertThat(focused(deleteFrom(1, "/dialog?sort=name"))).isEqualTo("member-3-actions-trigger");
    assertThat(focused(deleteFrom(3, "/dialog"))).as("the last row: the one before it")
        .isEqualTo("member-2-actions-trigger");
    Document last = deleteFrom(2, "/dialog");
    assertThat(focused(last)).isEqualTo("invite-member-button");
    Element invite = last.getElementById("invite-member-button");
    assertThat(invite.attr("hx-swap-oob")).isEqualTo("true");
    assertThat(invite.attr("commandfor")).isEqualTo("invite-member");
    assertThat(invite.text()).isEqualTo("Invite member");
  }

  private Document deleteFrom(long id, String page) throws Exception {
    return Jsoup.parseBodyFragment("<table>" + mockMvc.perform(delete("/dialog/members/" + id)
            .header("HX-Request", "true")
            .header("HX-Current-URL", "http://localhost" + page))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString() + "</table>");
  }

  private static String focused(Document fragment) {
    assertThat(fragment.select("[autofocus]")).hasSize(1);
    return fragment.selectFirst("[autofocus]").id();
  }
}
