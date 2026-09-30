package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The form page with htmx: the form of {@link FormPageTest}, which htmx posts and gets the {@code contact} fragment
 * back for. Without htmx it posts as a plain form.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HtmxFormPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theFormPostsWithHtmxAndWithoutIt() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/htmx-form"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    Element form = page.selectFirst("#contact > form");
    assertThat(form.attr("action")).isEqualTo("/htmx-form");
    assertThat(form.attr("method")).isEqualTo("post");
    assertThat(form.attr("hx-post")).isEqualTo("/htmx-form");
    assertThat(form.attr("hx-target")).isEqualTo("#contact");
    assertThat(form.attr("hx-swap")).isEqualTo("outerHTML");
    assertThat(form.select(".field")).as("the fields of the form page").hasSize(7);
    assertThat(page.select("[autofocus]")).isEmpty();
  }

  @Test
  void htmxLoadsItsPinnedWebjarFromTheApplication() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/htmx-form")).andReturn().getResponse().getContentAsString());
    String src = page.selectFirst("head script[src*=htmx]").attr("src");
    assertThat(src).as("the template leaves the version out; the webjar locator puts it in")
        .isEqualTo("/webjars/htmx.org/2.0.11/dist/htmx.min.js");
    assertThat(page.selectFirst("meta[name=htmx-config]").attr("content"))
        .contains("\"includeIndicatorStyles\": false", "\"allowEval\": false");

    mockMvc.perform(get(src))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("version:\"2.0.11\"")));
  }

  @Test
  void anHtmxSubmitWithErrorsGetsTheFragmentBackWithTheFocusOnTheFirstFieldWithAnError() throws Exception {
    String html = mockMvc.perform(htmx(post("/htmx-form")
            .param("name", "Wim")
            .param("email", "wim@example.com")
            .param("message", "Too short")
            .param("_terms", "on")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    assertThat(html).doesNotContain("<html", "<head", "<title", "page-header");
    Document fragment = Jsoup.parseBodyFragment(html);
    assertThat(fragment.body().children()).extracting(Element::id).containsExactly("contact");
    assertThat(fragment.select("#contact > form[hx-post=/htmx-form]")).hasSize(1);
    assertThat(fragment.select(".form-errors")).isEmpty();
    assertThat(fragment.select("[autofocus]")).extracting(Element::id).containsExactly("topic");
    assertThat(fragment.getElementById("topic").attr("aria-invalid")).isEqualTo("true");
    assertThat(fragment.getElementById("name").val()).isEqualTo("Wim");
    assertThat(fragment.select(".field-error[role], .form-errors[role]"))
        .as("focus reads the error out; a live region would say it twice").isEmpty();
  }

  @Test
  void anHtmxSubmitWithAGlobalErrorFocusesTheSummary() throws Exception {
    Document fragment = Jsoup.parseBodyFragment(mockMvc.perform(htmx(post("/htmx-form")
            .param("name", "Wim")
            .param("email", "wim@example.com")
            .param("topic", "support")
            .param("message", "Great deals at https://example.com/deals")
            .param("replyBy", "email")
            .param("terms", "true")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    Element errors = fragment.getElementById("contactForm-errors");
    assertThat(fragment.select("[autofocus]")).containsExactly(errors);
  }

  @Test
  void aValidHtmxSubmitSwapsInTheConfirmationWithoutARedirect() throws Exception {
    String html = mockMvc.perform(htmx(post("/htmx-form")
            .param("name", "Wim")
            .param("email", "wim@example.com")
            .param("topic", "support")
            .param("message", "Please call me about my order.")
            .param("replyBy", "phone")
            .param("terms", "true")))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    Document fragment = Jsoup.parseBodyFragment(html);
    assertThat(fragment.body().children()).extracting(Element::id).containsExactly("contact");
    Element confirmation = fragment.selectFirst("#contact > .alert");
    assertThat(confirmation.text()).contains("wim@example.com");
    assertThat(fragment.select("[autofocus]")).containsExactly(confirmation);
    assertThat(fragment.select("form")).isEmpty();
  }

  @Test
  void withoutHtmxAFailedSubmitGetsThePageBackWithTheSameFocus() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(post("/htmx-form")
            .param("name", "Wim")
            .param("email", "wim@example.com"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    assertThat(page.select("head script[src*=htmx]")).hasSize(1);
    assertThat(page.select("[autofocus]")).extracting(Element::id).containsExactly("topic");
  }

  @Test
  void withoutHtmxAValidSubmitRedirectsToTheConfirmation() throws Exception {
    mockMvc.perform(post("/htmx-form")
            .param("name", "Wim")
            .param("email", "wim@example.com")
            .param("topic", "support")
            .param("message", "Please call me about my order.")
            .param("replyBy", "phone")
            .param("terms", "true"))
        .andExpect(redirectedUrl("/htmx-form"))
        .andExpect(flash().attribute("sentTo", "wim@example.com"));

    Document page = Jsoup.parse(mockMvc.perform(get("/htmx-form").flashAttr("sentTo", "wim@example.com"))
        .andReturn().getResponse().getContentAsString());
    Element confirmation = page.selectFirst("#contact > .alert");
    assertThat(confirmation.text()).contains("wim@example.com");
    assertThat(confirmation.attr("tabindex")).isEqualTo("-1");
    assertThat(page.select("[autofocus]")).containsExactly(confirmation);
    assertThat(page.select("form")).isEmpty();
  }

  private static MockHttpServletRequestBuilder htmx(MockHttpServletRequestBuilder request) {
    return request.header("HX-Request", "true");
  }
}
