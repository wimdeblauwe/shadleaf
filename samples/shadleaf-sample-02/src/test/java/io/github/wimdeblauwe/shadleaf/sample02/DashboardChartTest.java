package io.github.wimdeblauwe.shadleaf.sample02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The dashboard's chart under the strict policy: its rows are a JSON data block, which the policy does not apply to
 * as it never runs, and the page has no inline style for its colours (the chart sets them through CSSOM). That the
 * chart then draws without a violation is checked in a browser by the docs' chart.spec.ts, under the same policy.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardChartTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theChartsRowsAreADataBlockWithTheMonthsAsIsoStrings() throws Exception {
    Element chart = Jsoup.parse(mockMvc.perform(get("/").with(user("user")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString())
        .getElementById("sign-ins-chart");

    assertThat(chart.tagName()).isEqualTo("sl-chart");
    assertThat(chart.select("> sl-chart-bar").eachAttr("key")).containsExactly("web", "app");
    assertThat(chart.select("[style]")).isEmpty();
    Element data = chart.selectFirst("> script");
    assertThat(data.attr("type")).isEqualTo("application/json");
    assertThat(data.hasAttr("nonce")).as("a data block needs no nonce").isFalse();
    JsonNode rows = JsonMapper.builder().build().readTree(data.data());
    assertThat(rows.size()).isEqualTo(6);
    assertThat(rows.get(0).get("month").asString()).isEqualTo("2026-05");
    assertThat(rows.get(0).get("web").asInt()).isEqualTo(186);
  }
}
