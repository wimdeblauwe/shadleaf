package io.github.wimdeblauwe.shadleaf.calendar;

import java.time.YearMonth;
import java.util.List;

/** The month {@code sl:month-view} shows: its caption, the column headers and the weeks. */
public final class CalendarMonth {

  private final YearMonth month;
  private final String caption;
  private final List<CalendarWeekday> weekdays;
  private final List<CalendarWeek> weeks;

  CalendarMonth(YearMonth month, String caption, List<CalendarWeekday> weekdays, List<CalendarWeek> weeks) {
    this.month = month;
    this.caption = caption;
    this.weekdays = weekdays;
    this.weeks = weeks;
  }

  /** The month. */
  public YearMonth getMonth() {
    return month;
  }

  /** The month's name and year in the page's locale ({@code October 2026}). */
  public String getCaption() {
    return caption;
  }

  /** The month before, as {@code 2026-09}, the value of the previous month's link. */
  public String getPrevious() {
    return month.minusMonths(1).toString();
  }

  /** The month after, as {@code 2026-11}, the value of the next month's link. */
  public String getNext() {
    return month.plusMonths(1).toString();
  }

  /** The column headers, from the first day of the week on. */
  public List<CalendarWeekday> getWeekdays() {
    return weekdays;
  }

  /** The weeks that hold a day of the month, four to six. */
  public List<CalendarWeek> getWeeks() {
    return weeks;
  }
}
