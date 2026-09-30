package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The settings page: link tabs whose active tab comes from {@code ?tab=}, a sessions panel htmx loads on first view
 * unless the server renders it, a collapsible, and an FAQ accordion with the question from {@code ?question=} open.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SettingsPageTest {

  @Autowired
  private MockMvc mockMvc;

  private Document page(String url) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(url))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }

  @Test
  void theAccountTabIsActiveByDefaultAndTheTabsAreLinks() throws Exception {
    Document page = page("/settings");

    Element tabs = page.getElementById("settings");
    assertThat(tabs.attr("x-data")).isEqualTo("slTabs");
    assertThat(tabs.attr("data-activation")).isEqualTo("manual");
    assertThat(page.select("#settings [role=tab]").eachAttr("href")).containsExactly("/settings?tab=account",
        "/settings?tab=notifications", "/settings?tab=sessions", "/settings?tab=help");
    assertThat(page.select("#settings [role=tab][aria-selected=true]").eachAttr("id"))
        .containsExactly("settings-account-trigger");
    assertThat(page.select("#settings [role=tabpanel]:not([hidden])").eachAttr("id"))
        .containsExactly("settings-account-content");
    assertThat(page.select("[id]").eachAttr("id")).doesNotHaveDuplicates();
  }

  @Test
  void theTabComesFromTheAddress() throws Exception {
    Document page = page("/settings?tab=help");

    assertThat(page.select("#settings [role=tab][aria-selected=true]").eachAttr("id"))
        .containsExactly("settings-help-trigger");
    assertThat(page.select("#settings [role=tabpanel]:not([hidden])").eachAttr("id"))
        .containsExactly("settings-help-content");
  }

  @Test
  void anUnknownTabFallsBackToTheFirst() throws Exception {
    assertThat(page("/settings?tab=nope").select("[role=tab][aria-selected=true]").eachAttr("id"))
        .containsExactly("settings-account-trigger");
  }

  @Test
  void theSessionsPanelLoadsOnFirstViewUnlessItIsActive() throws Exception {
    Element lazy = page("/settings").getElementById("settings-sessions-content");
    assertThat(lazy.attr("hx-get")).isEqualTo("/settings/sessions");
    assertThat(lazy.attr("hx-trigger")).isEqualTo("sl-tabs-show once");
    assertThat(lazy.getElementById("sessions")).isNull();

    Element rendered = page("/settings?tab=sessions").getElementById("settings-sessions-content");
    assertThat(rendered.hasAttr("hx-get")).isFalse();
    assertThat(rendered.select("#sessions li")).hasSize(2);

    Document fragment = Jsoup.parseBodyFragment(mockMvc.perform(get("/settings/sessions").header("HX-Request", "true"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    assertThat(fragment.body().children()).hasSize(1);
    assertThat(fragment.select("#sessions li")).hasSize(2);
    assertThat(fragment.select("nav, main")).as("the fragment alone, no layout").isEmpty();
  }

  @Test
  void theFaqIsASingleAccordionWithTheAskedQuestionOpen() throws Exception {
    Document page = page("/settings?tab=help&question=export");

    var items = page.select("#settings-help-content details.accordion-item");
    assertThat(items.eachAttr("id")).containsExactly("password", "export", "delete");
    assertThat(items.eachAttr("name")).hasSize(3).allMatch(name -> name.equals(items.first().attr("name")))
        .allMatch(name -> name.startsWith("sl-accordion-"));
    assertThat(page.select("details.accordion-item[open]").eachAttr("id")).containsExactly("export");
    assertThat(page.select("#export > summary [role=heading]").text()).isEqualTo("Can I export my data?");
  }

  @Test
  void theNotificationsTabHasACollapsible() throws Exception {
    Element collapsible = page("/settings?tab=notifications").selectFirst("details.collapsible");

    assertThat(collapsible.hasAttr("open")).isFalse();
    assertThat(collapsible.child(0).tagName()).isEqualTo("summary");
    assertThat(collapsible.child(0).classNames()).contains("btn", "collapsible-trigger");
    assertThat(collapsible.select(".collapsible-content .switch")).hasSize(1);
  }
}
