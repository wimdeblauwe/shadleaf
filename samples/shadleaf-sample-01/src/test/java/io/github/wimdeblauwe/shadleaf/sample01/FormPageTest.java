package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** The form page: Shadleaf's form controls bound with th:field, validated on the server. */
@SpringBootTest
@AutoConfigureMockMvc
class FormPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void emptyFormHasLabelledControlsAndNoErrors() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/form"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    for (String id : new String[] {"name", "email", "topic", "callbackDate", "message"}) {
      Element control = page.getElementById(id);
      assertThat(control).as("control #%s", id).isNotNull();
      assertThat(page.select("label.label[for=%s]".formatted(id))).as("label for #%s", id).hasSize(1);
      assertThat(control.hasAttr("aria-invalid")).isFalse();
      assertThat(control.hasAttr("aria-describedby")).isFalse();
    }
    assertThat(page.getElementById("email").attr("type")).isEqualTo("email");
    assertThat(page.getElementById("callbackDate").attr("type")).isEqualTo("date");
    assertThat(page.select("#topic option")).hasSize(4);
    assertThat(page.selectFirst(".native-select-wrapper").hasClass("form-full")).isTrue();
    assertThat(page.select(".radio-group[role=radiogroup] input.radio-group-item"))
        .extracting(Element::id).containsExactly("replyBy1", "replyBy2");
    assertThat(page.select("input.switch[role=switch][name=newsletter]")).hasSize(1);
    assertThat(page.select("label.label input.checkbox[name=terms]")).hasSize(1);
    assertThat(page.select("input[type=hidden][name=_terms], input[type=hidden][name=_newsletter]")).hasSize(2);
    assertThat(page.select(".form-error")).isEmpty();
    assertThat(page.select("sl|input, sl|label, sl|textarea, sl|native-select")).isEmpty();
  }

  @Test
  void invalidSubmitMarksTheFieldsWithErrorsAndKeepsTheInput() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/form")
            .param("name", "Wim")
            .param("email", "not-an-email")
            .param("topic", "")
            .param("callbackDate", "2026-10-15")
            .param("message", "Too short")
            .param("newsletter", "true")
            .param("_newsletter", "on")
            .param("_terms", "on"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    Element name = page.getElementById("name");
    assertThat(name.val()).isEqualTo("Wim");
    assertThat(name.hasAttr("aria-invalid")).isFalse();
    assertThat(page.getElementById("callbackDate").val()).isEqualTo("2026-10-15");

    for (String id : new String[] {"email", "topic", "message"}) {
      Element control = page.getElementById(id);
      assertThat(control.attr("aria-invalid")).as("#%s aria-invalid", id).isEqualTo("true");
      assertThat(control.attr("aria-describedby")).isEqualTo(id + "-error");
      assertThat(page.getElementById(id + "-error").text()).isNotBlank();
    }
    assertThat(page.getElementById("email").val()).isEqualTo("not-an-email");
    assertThat(page.getElementById("message").text()).isEqualTo("Too short");

    assertThat(page.select("input.radio-group-item")).allSatisfy(item ->
        assertThat(item.attr("aria-invalid")).isEqualTo("true"));
    Element replyBy = page.selectFirst(".radio-group");
    assertThat(replyBy.attr("aria-labelledby")).isEqualTo("replyBy-question");
    assertThat(replyBy.attr("aria-describedby")).isEqualTo("replyBy-error");
    Element terms = page.getElementById("terms1");
    assertThat(terms.attr("aria-invalid")).isEqualTo("true");
    assertThat(terms.attr("aria-describedby")).isEqualTo("terms-error");
    assertThat(page.getElementById("terms-error").text()).isEqualTo("must be accepted");
    Element newsletter = page.getElementById("newsletter1");
    assertThat(newsletter.hasAttr("checked")).as("the switch keeps the submitted value").isTrue();
    assertThat(newsletter.hasAttr("aria-invalid")).isFalse();
  }

  @Test
  void validSubmitRedirectsWithTheConfirmation() throws Exception {
    mockMvc.perform(post("/form")
            .param("name", "Wim")
            .param("email", "wim@example.com")
            .param("topic", "support")
            .param("message", "Please call me about my order.")
            .param("replyBy", "phone")
            .param("terms", "true"))
        .andExpect(redirectedUrl("/form"))
        .andExpect(flash().attribute("sentTo", "wim@example.com"));

    Document page = Jsoup.parse(mockMvc.perform(get("/form").flashAttr("sentTo", "wim@example.com"))
        .andReturn().getResponse().getContentAsString());
    assertThat(page.selectFirst(".alert[role=status]").text()).contains("wim@example.com");
  }
}
