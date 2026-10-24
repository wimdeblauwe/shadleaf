package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The charts page: sl:charts over the people table's aggregates, a series per role, the months as YearMonths, linked
 * from the sidebar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChartsPageTest {

  private static final List<String> ROLES = List.of("owner", "admin", "member", "guest");

  @Autowired
  private MockMvc mockMvc;

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void everyChartHasASeriesPerRole() throws Exception {
    Document page = page();

    assertThat(page.select("sl-chart").eachAttr("id"))
        .containsExactly("joined-chart", "members-chart", "new-chart", "orders-chart");
    for (String id : List.of("joined-chart", "members-chart", "new-chart")) {
      Element chart = page.getElementById(id);
      assertThat(chart.select("> [key]").not("sl-chart-x-axis").eachAttr("key")).as(id).isEqualTo(ROLES);
      assertThat(chart.select("> [key]").not("sl-chart-x-axis").eachAttr("label")).as(id)
          .containsExactly("Owner", "Admin", "Member", "Guest");
    }
    assertThat(page.getElementById("joined-chart").select("> sl-chart-bar").eachAttr("stack")).containsOnly("roles");
  }

  @Test
  void theLastTwelveMonthsCountWhoJoinedEachMonth() throws Exception {
    JsonNode rows = data(page(), "joined-chart");

    assertThat(rows.size()).isEqualTo(12);
    assertThat(rows.get(0).get("month").asString()).matches("\\d{4}-\\d{2}");
    long joined = 0;
    for (JsonNode row : rows) {
      for (String role : ROLES) {
        joined += row.get(role).asLong();
      }
    }
    assertThat(joined).isPositive();
  }

  @Test
  void membersOverTimeAddUpToEveryone() throws Exception {
    JsonNode rows = data(page(), "members-chart");

    JsonNode last = rows.get(rows.size() - 1);
    long everyone = 0;
    for (String role : ROLES) {
      everyone += last.get(role).asLong();
    }
    assertThat(everyone).isEqualTo(270);
    List<String> months = new ArrayList<>();
    rows.forEach(row -> months.add(row.get("month").asString()));
    assertThat(months).isSorted().doesNotHaveDuplicates();
  }

  @Test
  void ordersPerRoleAreCategoriesWithTheRolesLabel() throws Exception {
    JsonNode rows = data(page(), "orders-chart");

    List<String> labels = new ArrayList<>();
    rows.forEach(row -> labels.add(row.get("label").asString()));
    assertThat(labels).containsExactly("Owner", "Admin", "Member", "Guest");
    assertThat(rows.get(2).get("orders").asLong()).isPositive();
  }

  @Test
  void theSidebarLinksThePage() throws Exception {
    Element link = page().selectFirst(".sidebar a[href=\"/charts\"]");

    assertThat(link).isNotNull();
    assertThat(link.attr("aria-current")).isEqualTo("page");
  }

  private Document page() throws Exception {
    return Jsoup.parse(mockMvc.perform(get("/charts")).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());
  }

  private JsonNode data(Document page, String id) {
    return jsonMapper.readTree(page.getElementById(id).selectFirst("> script[type=application/json]").data());
  }
}
