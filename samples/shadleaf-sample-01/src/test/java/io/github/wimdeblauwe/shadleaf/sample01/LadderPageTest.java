package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** The index page demonstrates the adjustability ladder with <sl:button>; this checks what it renders. */
@SpringBootTest
@AutoConfigureMockMvc
class LadderPageTest {

  @Autowired
  private MockMvc mockMvc;

  private Document page;

  @BeforeEach
  void renderThePage() throws Exception {
    page = Jsoup.parse(mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }

  @Test
  void propsRenderAsDataAttributesAndNeverLeak() {
    assertThat(page.select(".button[data-variant]")).extracting(button -> button.attr("data-variant"))
        .contains("secondary", "outline", "ghost", "link", "destructive")
        .doesNotContain("default");
    assertThat(page.select(".button")).allSatisfy(button -> assertThat(button.attributes().asList())
        .extracting(Attribute::getKey)
        .doesNotContain("variant", "size", "as", "loading", "th:variant", "th:size"));
    assertThat(page.select("sl|button, sl|icon, sl|slot")).isEmpty();
  }

  @Test
  void iconOnlyButtonsHaveAnAccessibleName() {
    assertThat(page.select(".button[data-size^=icon]"))
        .hasSizeGreaterThanOrEqualTo(8)
        .allSatisfy(button -> assertThat(button.attr("aria-label")).isNotBlank());
  }

  @Test
  void loadingButtonsAnnounceTheBusyStateWithTheBuiltInString() {
    assertThat(page.select(".button[aria-busy=true]")).hasSizeGreaterThanOrEqualTo(3).allSatisfy(button -> {
      assertThat(button.hasAttr("disabled")).isTrue();
      assertThat(button.selectFirst(".button-spinner")).isNotNull();
      assertThat(button.selectFirst(".sl-sr-only").text()).isEqualTo("Loading");
    });
    assertThat(page.html()).doesNotContain("??sl.");
  }

  @Test
  void disabledLinkCannotBeFollowed() {
    Element link = page.selectFirst("a.button[aria-disabled=true]");

    assertThat(link.hasAttr("href")).isFalse();
    assertThat(link.attr("tabindex")).isEqualTo("-1");
    assertThat(page.selectFirst("a.button[data-variant=outline]").attr("href")).isEqualTo("/");
  }

  @Test
  void passThroughAttributesAndClassesLandOnTheButton() {
    Element sync = page.selectFirst(".button[hx-post]");

    assertThat(sync.className()).isEqualTo("button wide");
    assertThat(sync.attr("hx-post")).isEqualTo("/orders/sync");
    assertThat(sync.attr("hx-target")).isEqualTo("#orders");
    assertThat(sync.attr("x-on:click")).isEqualTo("syncing = true");
    assertThat(sync.attr("data-order-count")).isEqualTo("3");
    assertThat(sync.attr("aria-describedby")).isEqualTo("sync-help");
    assertThat(sync.selectFirst("> .button-icon[data-icon=inline-start] > svg.icon")).isNotNull();
  }

  @Test
  void headerHasTheLibrarysThemeToggle() {
    assertThat(page.select("header .theme-toggle[x-data=slThemeToggle] > button.theme-toggle-trigger"))
        .singleElement()
        .satisfies(button -> {
          assertThat(button.attr("popovertarget")).isEqualTo("theme-toggle");
          assertThat(button.attr("aria-label")).isEqualTo("Theme");
        });
    assertThat(page.select("#theme-toggle [data-theme-choice]")).extracting(item -> item.attr("data-theme-choice"))
        .containsExactly("light", "dark", "system");
  }

  @Test
  void applicationIconReplacesTheBundledOne() {
    Element leaf = page.selectFirst("svg.icon[aria-label=Shadleaf]");

    assertThat(leaf.attr("role")).isEqualTo("img");
    assertThat(leaf.attr("fill")).as("the sample's filled leaf, not lucide's stroked one").isEqualTo("currentColor");
    assertThat(leaf.hasAttr("stroke")).isFalse();
    assertThat(leaf.attr("width")).isEqualTo("32");
  }
}
