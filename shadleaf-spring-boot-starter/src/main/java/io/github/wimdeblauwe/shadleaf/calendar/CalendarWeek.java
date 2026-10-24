package io.github.wimdeblauwe.shadleaf.calendar;

import java.util.List;

/** One row of {@code sl:month-view}: seven days, from the first day of the week on. */
public final class CalendarWeek {

  private final int number;
  private final List<CalendarDay> days;

  CalendarWeek(int number, List<CalendarDay> days) {
    this.number = number;
    this.days = days;
  }

  /** The week number, as the locale counts weeks (ISO weeks in most of Europe). */
  public int getNumber() {
    return number;
  }

  /** The seven days. */
  public List<CalendarDay> getDays() {
    return days;
  }
}
