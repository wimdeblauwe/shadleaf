package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The leave page: a range of days picked in a calendar, two months side by side. The calendar disables the weekends,
 * the public holidays and the days before today, and the controller checks the same, since the calendar only helps
 * the user: the first and the last day must be working days from today on. A valid request redirects back to an empty
 * form with a toast that counts the working days in it (post/redirect/get).
 */
@Controller
public class LeaveController {

  /** Belgium's public holidays on working days, in this sample's range of dates. */
  static final List<LocalDate> PUBLIC_HOLIDAYS = Stream.of("2026-11-11", "2026-12-25", "2027-01-01", "2027-03-29",
      "2027-05-06", "2027-05-17", "2027-07-21", "2027-11-01", "2027-11-11").map(LocalDate::parse).toList();

  @ModelAttribute("today")
  LocalDate today() {
    return LocalDate.now();
  }

  @ModelAttribute("publicHolidays")
  List<LocalDate> publicHolidays() {
    return PUBLIC_HOLIDAYS;
  }

  /** For the calendar's disabled-dates: the weekends and the public holidays. */
  @ModelAttribute("closedDays")
  String closedDays() {
    return Stream.concat(Stream.of("sat", "sun"), PUBLIC_HOLIDAYS.stream().map(LocalDate::toString))
        .collect(Collectors.joining(","));
  }

  @GetMapping("/leave")
  public String form(@ModelAttribute("leaveRequest") LeaveRequest leaveRequest) {
    return "leave";
  }

  @PostMapping("/leave")
  public String submit(@Valid @ModelAttribute("leaveRequest") LeaveRequest leaveRequest, BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    rejectClosedDays(leaveRequest, bindingResult, today());
    if (bindingResult.hasErrors()) {
      return "leave";
    }
    long days = workingDays(leaveRequest.getFirstDay(), leaveRequest.getLastDay());
    redirectAttributes.addFlashAttribute("toasts", List.of(Toast.success("Leave requested")
        .withDescription("%d working day%s, from %s to %s.".formatted(days, days == 1 ? "" : "s",
            leaveRequest.getFirstDay(), leaveRequest.getLastDay()))));
    return "redirect:/leave";
  }

  /** The first and the last day must be working days from today on, the last not before the first. */
  static void rejectClosedDays(LeaveRequest leaveRequest, BindingResult bindingResult, LocalDate today) {
    LocalDate first = leaveRequest.getFirstDay();
    LocalDate last = leaveRequest.getLastDay();
    if (first != null && (first.isBefore(today) || !isWorkingDay(first))) {
      bindingResult.rejectValue("firstDay", "leaveRequest.closed", "The first day must be a working day from today on.");
    }
    if (last != null && (last.isBefore(today) || !isWorkingDay(last))) {
      bindingResult.rejectValue("lastDay", "leaveRequest.closed", "The last day must be a working day from today on.");
    }
    if (first != null && last != null && last.isBefore(first)) {
      bindingResult.reject("leaveRequest.order", "The last day comes before the first.");
    }
  }

  static boolean isWorkingDay(LocalDate date) {
    return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY
        && !PUBLIC_HOLIDAYS.contains(date);
  }

  static long workingDays(LocalDate first, LocalDate last) {
    return first.datesUntil(last.plusDays(1)).filter(LeaveController::isWorkingDay).count();
  }
}
