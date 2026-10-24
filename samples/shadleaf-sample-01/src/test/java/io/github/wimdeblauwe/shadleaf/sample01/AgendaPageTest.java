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
 * The team agenda: a sl:month-view with each day's events as badges, days linking to {@code ?date=}, which lists the
 * day's events, and the months linked with {@code ?month=}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AgendaPageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theMonthShowsTheEventsOfEachDayAndLinksEveryDay() throws Exception {
    Document page = page("/agenda?month=2026-11");

    assertThat(page.select(".month-view caption").text()).isEqualTo("November 2026");
    assertThat(dayBadges(page, "2026-11-11")).isEqualTo("Public holiday");
    assertThat(dayBadges(page, "2026-11-16")).isEqualTo("Stand-up");
    assertThat(dayBadges(page, "2026-11-04")).isEqualTo("Planning");
    assertThat(dayBadges(page, "2026-11-27")).isEqualTo("Demo Retro");
    assertThat(page.select("a.month-view-previous").attr("href")).isEqualTo("/agenda?month=2026-10");
    assertThat(page.select("a.month-view-next").attr("href")).isEqualTo("/agenda?month=2026-12");
    assertThat(page.getElementById("agenda-day")).isNull();
  }

  @Test
  void aChosenDayListsItsEventsInItsMonth() throws Exception {
    Document page = page("/agenda?date=2026-11-27");

    assertThat(page.select(".month-view caption").text()).isEqualTo("November 2026");
    Element day = page.getElementById("agenda-day");
    assertThat(day.select(".card-title").text()).isEqualTo("Friday 27 November 2026");
    assertThat(day.select("li").eachText()).containsExactly("Demo", "Retro");
    assertThat(page.select("a.month-view-next").attr("href"))
        .as("the next month keeps the chosen day").isEqualTo("/agenda?date=2026-11-27&month=2026-12");
  }

  @Test
  void theMonthParameterWinsOverTheChosenDay() throws Exception {
    Document page = page("/agenda?date=2026-11-27&month=2026-12");

    assertThat(page.select(".month-view caption").text()).isEqualTo("December 2026");
    assertThat(page.getElementById("agenda-day").select(".card-content").text()).isEqualTo("Demo Retro");
  }

  private Document page(String url) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());
  }

  private static String dayBadges(Document page, String date) {
    return page.select("a.month-view-day-link[href=\"?date=%s\"] .badge".formatted(date)).text();
  }
}
