package io.github.wimdeblauwe.shadleaf.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.IExpressionContext;

/**
 * The expression object {@code #slCalendar}: what {@code sl:calendar} needs from the locale of the page being rendered
 * (Spring's {@code LocaleResolver}), so the calendar the browser draws follows the server's locale, not the browser's.
 * The names of the months and days come from the browser's {@code Intl} for the same language tag. It also formats the
 * date {@code sl:date-picker} shows on its button before {@code slDatePicker} formats it again with {@code Intl}.
 */
public final class CalendarLocale {

  private final Locale locale;

  public CalendarLocale(IExpressionContext context) {
    this(context.getLocale());
  }

  CalendarLocale(Locale locale) {
    this.locale = locale;
  }

  /** The locale as a BCP 47 language tag, for {@code Intl}: {@code en-US}, {@code nl-BE}. */
  public String languageTag() {
    return locale.toLanguageTag();
  }

  /** The first day of the week, counted as react-day-picker's {@code weekStartsOn}: 0 is Sunday, 1 Monday, ... */
  public int weekStartsOn() {
    return dayNumber(WeekFields.of(locale).getFirstDayOfWeek());
  }

  /**
   * How many days of the new year the first week needs, for the week numbers: 4 for ISO weeks (most of Europe), 1
   * where the week holding 1 January is the first (the United States).
   */
  public int firstWeekMinDays() {
    return WeekFields.of(locale).getMinimalDaysInFirstWeek();
  }

  /**
   * An ISO date ({@code 2026-10-23}) in the page's locale, in a {@link FormatStyle} named as {@code Intl}'s
   * {@code dateStyle} ({@code short}, {@code medium}, {@code long}, {@code full}): {@code October 23, 2026} in the long
   * style. Both use the CLDR's patterns, so they agree. {@code null} for a blank value or one that is no ISO date, which
   * a date input shows as empty.
   */
  public @Nullable String format(@Nullable String iso, String style) {
    if (iso == null || iso.isBlank()) {
      return null;
    }
    try {
      return DateTimeFormatter.ofLocalizedDate(FormatStyle.valueOf(style.toUpperCase(Locale.ROOT)))
          .withLocale(locale)
          .format(LocalDate.parse(iso));
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static int dayNumber(DayOfWeek day) {
    return day.getValue() % 7;
  }
}
