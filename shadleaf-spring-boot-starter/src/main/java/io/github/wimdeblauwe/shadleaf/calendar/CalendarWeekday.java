package io.github.wimdeblauwe.shadleaf.calendar;

/** A column header of {@code sl:month-view}. */
public final class CalendarWeekday {

  private final String shortName;
  private final String longName;

  CalendarWeekday(String shortName, String longName) {
    this.shortName = shortName;
    this.longName = longName;
  }

  /** The name shown: two letters ({@code Mo}), or the narrow name where two letters do not tell the days apart. */
  public String getShortName() {
    return shortName;
  }

  /** The whole name ({@code Monday}), for screen readers. */
  public String getLongName() {
    return longName;
  }
}
