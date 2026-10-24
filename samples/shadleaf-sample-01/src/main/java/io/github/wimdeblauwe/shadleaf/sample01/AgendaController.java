package io.github.wimdeblauwe.shadleaf.sample01;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The team agenda: a sl:month-view with the events of each day, the public holidays of the leave page among them.
 * Every day links to {@code ?date=2026-10-23}, which lists that day's events under the month. The month shown is the
 * {@code month} parameter the month view's links set, else the month of the chosen day, else the current month. The
 * events repeat every month (a stand-up every Monday, a demo on the last Friday), so every month has some.
 */
@Controller
public class AgendaController {

  @GetMapping("/agenda")
  public String agenda(@RequestParam(required = false) @Nullable String month,
      @RequestParam(required = false) @Nullable String date, Model model) {
    LocalDate chosen = parseDate(date);
    YearMonth shown = parseMonth(month);
    if (shown == null) {
      shown = chosen != null ? YearMonth.from(chosen) : YearMonth.now();
    }
    Map<LocalDate, List<String>> events = events(shown);
    model.addAttribute("month", shown);
    model.addAttribute("events", events);
    model.addAttribute("chosen", chosen);
    model.addAttribute("chosenEvents", chosen == null ? List.of() : events(YearMonth.from(chosen))
        .getOrDefault(chosen, List.of()));
    return "agenda";
  }

  /** The events of every day of the month and of the days of the weeks around it that the month view shows. */
  static Map<LocalDate, List<String>> events(YearMonth month) {
    Map<LocalDate, List<String>> events = new TreeMap<>();
    LocalDate from = month.atDay(1).minusDays(7);
    LocalDate to = month.atEndOfMonth().plusDays(7);
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      List<String> today = new ArrayList<>();
      if (LeaveController.PUBLIC_HOLIDAYS.contains(day)) {
        today.add("Public holiday");
      } else if (day.getDayOfWeek() == DayOfWeek.MONDAY) {
        today.add("Stand-up");
      }
      if (day.getDayOfWeek() == DayOfWeek.WEDNESDAY && day.getDayOfMonth() <= 7) {
        today.add("Planning");
      }
      if (day.getDayOfWeek() == DayOfWeek.FRIDAY && day.plusWeeks(1).getMonth() != day.getMonth()) {
        today.add("Demo");
        today.add("Retro");
      }
      if (!today.isEmpty()) {
        events.put(day, today);
      }
    }
    return events;
  }

  private static @Nullable LocalDate parseDate(@Nullable String value) {
    try {
      return value == null || value.isBlank() ? null : LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static @Nullable YearMonth parseMonth(@Nullable String value) {
    try {
      return value == null || value.isBlank() ? null : YearMonth.parse(value);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
