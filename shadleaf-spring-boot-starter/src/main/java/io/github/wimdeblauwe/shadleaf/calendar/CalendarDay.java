package io.github.wimdeblauwe.shadleaf.calendar;

import java.time.LocalDate;

/**
 * One day of {@code sl:month-view}, which its {@code day} slot sees as {@code ${slDay}}: look up what the day shows
 * with {@code ${events[slDay.date]}}.
 */
public final class CalendarDay {

  private final LocalDate date;
  private final String label;
  private final boolean outside;
  private final boolean today;

  CalendarDay(LocalDate date, String label, boolean outside, boolean today) {
    this.date = date;
    this.label = label;
    this.outside = outside;
    this.today = today;
  }

  /** The date. */
  public LocalDate getDate() {
    return date;
  }

  /** The date as ISO ({@code 2026-10-23}), as {@code day-href} gets it in place of {@code {date}}. */
  public String getIso() {
    return date.toString();
  }

  /** The day of the month, which the cell shows. */
  public int getNumber() {
    return date.getDayOfMonth();
  }

  /** The whole date in the page's locale ({@code Friday, October 23, 2026}), for screen readers. */
  public String getLabel() {
    return label;
  }

  /** Whether it belongs to the previous or next month and only fills the first or last week. */
  public boolean isOutside() {
    return outside;
  }

  /** Whether it is today. */
  public boolean isToday() {
    return today;
  }
}
