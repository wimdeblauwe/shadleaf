package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Ladder rung 5: an application's own templates/sl/components/button.html replaces the library's, with no
 * configuration. The copy is in src/test/resources, so it applies to every test in this sample but not to the
 * running sample.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ButtonOverrideTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ComponentRegistry componentRegistry;

  @Test
  void everyButtonOnThePageComesFromTheApplicationsCopy() throws Exception {
    Document page = Jsoup.parse(mockMvc.perform(get("/"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    Elements buttons = page.select(".btn");
    assertThat(buttons).hasSizeGreaterThan(30);
    assertThat(buttons).allSatisfy(button -> assertThat(button.attr("data-ejected")).isEqualTo("shadleaf-sample-01"));
    assertThat(page.select("a.btn")).as("the as=\"a\" branch too").isNotEmpty();
  }

  @Test
  void theApplicationsCopyAlsoSuppliesTheProps() {
    assertThat(componentRegistry.get("button").source()).contains("test-classes");
    assertThat(componentRegistry.get("icon").source()).contains("shadleaf-spring-boot-starter");
  }
}
