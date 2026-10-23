package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The leave page: a range in a sl:calendar, its two date inputs bound to the first and the last day, with the weekends,
 * the public holidays and the past disabled in the calendar and rejected by the controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LeavePageTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void theCalendarHoldsTheTwoDateInputsAndDisablesTheClosedDays() throws Exception {
    Document page = page(mockMvc.perform(get("/leave")).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());

    Element calendar = page.selectFirst(".calendar");
    assertThat(calendar.attr("x-data")).isEqualTo("slCalendar");
    assertThat(calendar.attr("aria-label")).isEqualTo("Leave period");
    assertThat(calendar.attr("data-number-of-months")).isEqualTo("2");
    assertThat(calendar.attr("data-disabled-dates")).startsWith("sat,sun,2026-11-11,");
    assertThat(calendar.select("> input[type=date]")).extracting(Element::id, input -> input.attr("name"),
            input -> input.attr("min"), input -> input.attr("aria-label"))
        .containsExactly(
            tuple("firstDay", "firstDay", LocalDate.now().toString(), "First day"),
            tuple("lastDay", "lastDay", LocalDate.now().toString(), "Last day"));
    assertThat(page.select(".form-errors, [aria-invalid]")).isEmpty();
  }

  @Test
  void anEmptyRequestShowsBothMissingDays() throws Exception {
    Document page = page(mockMvc.perform(post("/leave")).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());

    assertThat(page.select(".form-errors li")).extracting(Element::text)
        .containsExactlyInAnyOrder("Pick the first day of your leave.", "Pick the last day of your leave.");
    assertThat(page.select(".calendar > input[aria-invalid=true]")).hasSize(2);
  }

  @Test
  void aWeekendOrAHolidayOrTheWrongOrderIsRejected() throws Exception {
    LocalDate saturday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
    LocalDate holiday = LeaveController.PUBLIC_HOLIDAYS.stream().filter(day -> day.isAfter(LocalDate.now()))
        .findFirst().orElseThrow();
    LocalDate first = nextWorkingDay(LocalDate.now().plusDays(7));
    LocalDate last = nextWorkingDay(first.plusDays(1));

    // One message shows as text, several as a list.
    Document weekend = submit(saturday, last);
    assertThat(weekend.selectFirst(".form-errors").text())
        .contains("The first day must be a working day from today on.");
    assertThat(weekend.getElementById("firstDay").attr("aria-invalid")).isEqualTo("true");
    assertThat(weekend.getElementById("firstDay").val()).isEqualTo(saturday.toString());

    assertThat(submit(first, holiday).selectFirst(".form-errors").text())
        .contains("The last day must be a working day from today on.");
    assertThat(submit(last, first).selectFirst(".form-errors").text()).contains("The last day comes before the first.");
  }

  @Test
  void aValidRequestRedirectsWithAToastCountingTheWorkingDays() throws Exception {
    LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    while (!LeaveController.isWorkingDay(monday) || !LeaveController.isWorkingDay(monday.plusDays(7))) {
      monday = monday.plusWeeks(1);
    }
    LocalDate nextMonday = monday.plusDays(7);
    long days = LeaveController.workingDays(monday, nextMonday);

    mockMvc.perform(post("/leave")
            .param("firstDay", monday.toString())
            .param("lastDay", nextMonday.toString()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/leave"))
        .andExpect(flash().attribute("toasts", List.of(Toast.success("Leave requested")
            .withDescription("%d working days, from %s to %s.".formatted(days, monday, nextMonday)))));
  }

  private Document submit(LocalDate first, LocalDate last) throws Exception {
    return page(mockMvc.perform(post("/leave")
            .param("firstDay", first.toString())
            .param("lastDay", last.toString()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }

  private static Document page(String html) {
    return Jsoup.parse(html);
  }

  private static LocalDate nextWorkingDay(LocalDate from) {
    LocalDate date = from;
    while (!LeaveController.isWorkingDay(date)) {
      date = date.plusDays(1);
    }
    return date;
  }
}
