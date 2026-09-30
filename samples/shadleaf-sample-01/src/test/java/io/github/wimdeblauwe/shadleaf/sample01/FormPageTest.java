package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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

/** The form page: Shadleaf's fields, each bound with one th:field, validated on the server, posted as a plain form. */
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

    for (String id : new String[] {"name", "email", "topic", "callbackDate", "message", "newsletter", "terms"}) {
      Element control = page.getElementById(id);
      assertThat(control).as("control #%s", id).isNotNull();
      assertThat(control.closest(".field")).as("#%s is in a field", id).isNotNull();
      assertThat(page.select("label.label.field-label[for=%s]".formatted(id))).as("label for #%s", id).hasSize(1);
      assertThat(control.hasAttr("aria-invalid")).isFalse();
    }
    assertThat(page.getElementById("name").hasAttr("aria-describedby")).isFalse();
    assertThat(page.getElementById("email").attr("aria-describedby")).isEqualTo("email-description");
    assertThat(page.getElementById("email-description").text()).isNotBlank();
    assertThat(page.getElementById("message").attr("aria-describedby")).isEqualTo("message-description");
    assertThat(page.getElementById("email").attr("type")).isEqualTo("email");
    assertThat(page.getElementById("callbackDate").attr("type")).isEqualTo("date");
    assertThat(page.select("#topic option")).hasSize(4);
    assertThat(page.select("fieldset.field-set .radio-group[role=radiogroup] input.radio-group-item"))
        .extracting(Element::id, item -> item.attr("name")).containsExactly(
            tuple("replyBy1", "replyBy"), tuple("replyBy2", "replyBy"));
    assertThat(page.selectFirst("fieldset.field-set").hasAttr("aria-describedby")).isFalse();
    assertThat(page.select("input.switch[role=switch][name=newsletter]")).hasSize(1);
    assertThat(page.select("input.checkbox[name=terms]")).hasSize(1);
    assertThat(page.select("input[type=hidden][name=_terms], input[type=hidden][name=_newsletter]")).hasSize(2);
    assertThat(page.select(".field-error, [data-invalid], .form-errors")).isEmpty();
    assertThat(page.select("[autofocus]")).as("the first, empty form keeps the focus where it was").isEmpty();
    Element form = page.selectFirst("form");
    assertThat(form.attr("action")).isEqualTo("/form");
    assertThat(form.attr("method")).isEqualTo("post");
    assertThat(form.attributes().asList()).noneMatch(attribute -> attribute.getKey().startsWith("hx-"));
    assertThat(page.select("*").stream().filter(element -> element.tagName().startsWith("sl:"))).isEmpty();
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
      assertThat(control.attr("aria-describedby")).as("#%s aria-describedby", id).endsWith(id + "-error");
      assertThat(control.closest(".field").attr("data-invalid")).isEqualTo("true");
      assertThat(page.getElementById(id + "-error").text()).isNotBlank();
    }
    assertThat(page.getElementById("email").attr("aria-describedby")).isEqualTo("email-description email-error");
    assertThat(page.getElementById("email").val()).isEqualTo("not-an-email");
    assertThat(page.getElementById("message").text()).isEqualTo("Too short");
    assertThat(name.closest(".field").hasAttr("data-invalid")).isFalse();
    assertThat(page.getElementById("name-error")).isNull();

    assertThat(page.select("input.radio-group-item")).allSatisfy(item ->
        assertThat(item.attr("aria-invalid")).isEqualTo("true"));
    Element replyBy = page.selectFirst("fieldset.field-set");
    assertThat(replyBy.attr("data-invalid")).isEqualTo("true");
    assertThat(replyBy.attr("aria-describedby")).isEqualTo("replyBy-error");
    assertThat(replyBy.selectFirst("legend").text()).isEqualTo("Reply by");
    assertThat(page.getElementById("replyBy-error").text()).isNotBlank();
    Element terms = page.getElementById("terms");
    assertThat(terms.attr("aria-invalid")).isEqualTo("true");
    assertThat(terms.attr("aria-describedby")).isEqualTo("terms-error");
    assertThat(page.getElementById("terms-error").text()).isEqualTo("must be accepted");
    Element newsletter = page.getElementById("newsletter");
    assertThat(newsletter.hasAttr("checked")).as("the switch keeps the submitted value").isTrue();
    assertThat(newsletter.hasAttr("aria-invalid")).isFalse();
    assertThat(page.select(".form-errors")).as("only field errors").isEmpty();
    assertThat(page.select("[autofocus]")).as("the first field with an error, in the order of the form")
        .extracting(Element::id).containsExactly("email");
  }

  @Test
  void aMessageWithALinkIsRejectedAsAWholeAtTheTopOfTheForm() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/form")
            .param("name", "Wim")
            .param("email", "wim@example.com")
            .param("topic", "support")
            .param("message", "Great deals at https://example.com/deals")
            .param("replyBy", "email")
            .param("terms", "true"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    Element errors = page.selectFirst("form > .form-errors");
    assertThat(errors).isNotNull();
    assertThat(errors.firstElementSibling()).as("the first thing in the form").isEqualTo(errors);
    assertThat(errors.id()).isEqualTo("contactForm-errors");
    assertThat(errors.attr("data-variant")).isEqualTo("destructive");
    assertThat(errors.attr("tabindex")).isEqualTo("-1");
    assertThat(page.select("[autofocus]")).containsExactly(errors);
    assertThat(errors.hasAttr("role")).isFalse();
    assertThat(errors.selectFirst(".alert-title").text()).isEqualTo("There is a problem");
    assertThat(errors.selectFirst(".alert-description").text())
        .isEqualTo("We do not accept messages with links, to keep spam out.");
    assertThat(page.select("[aria-invalid], [data-invalid], .field-error")).as("no field is at fault").isEmpty();
    assertThat(page.getElementById("message").text()).isEqualTo("Great deals at https://example.com/deals");
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
