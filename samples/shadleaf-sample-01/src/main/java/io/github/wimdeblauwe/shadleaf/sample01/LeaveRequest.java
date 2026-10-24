package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * The form backing object of the leave page: a range of days, the first and the last. ISO dates, as the calendar's date
 * inputs submit them.
 */
public class LeaveRequest {

  @NotNull(message = "Pick the first day of your leave.")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate firstDay;

  @NotNull(message = "Pick the last day of your leave.")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate lastDay;

  public LocalDate getFirstDay() {
    return firstDay;
  }

  public void setFirstDay(LocalDate firstDay) {
    this.firstDay = firstDay;
  }

  public LocalDate getLastDay() {
    return lastDay;
  }

  public void setLastDay(LocalDate lastDay) {
    this.lastDay = lastDay;
  }
}
