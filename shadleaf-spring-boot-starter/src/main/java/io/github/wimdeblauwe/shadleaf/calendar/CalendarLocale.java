package io.github.wimdeblauwe.shadleaf.calendar;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.IExpressionContext;

/**
 * The expression object {@code #slCalendar}: what {@code sl:calendar} needs from the locale of the page being rendered
 * (Spring's {@code LocaleResolver}), so the calendar the browser draws follows the server's locale, not the browser's.
 * The names of the months and days come from the browser's {@code Intl} for the same language tag. It also formats the
 * date {@code sl:date-picker} shows on its button before {@code slDatePicker} formats it again with {@code Intl}, and
 * lays out the month {@code sl:month-view} renders on the server.
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

  /**
   * The month {@code sl:month-view} shows, its weeks starting on the first day of the week.
   *
   * @param month        the month as {@code 2026-10}, or a date in it; {@code null} for {@code requested}
   * @param requested    the month the request asks for ({@code ?month=2026-10}); one that is no month is ignored, as
   *                     a visitor may have edited the URL
   * @param today        today as {@code 2026-10-23}; {@code null} for the server's date
   * @param weekStartsOn the first day of the week, 0 for Sunday to 6; {@code null} for the locale's
   */
  public CalendarMonth month(@Nullable String month, @Nullable String requested, @Nullable String today,
      @Nullable Number weekStartsOn) {
    LocalDate todayDate = today == null || today.isBlank() ? LocalDate.now() : parseDate("today", today);
    YearMonth shown = month != null && !month.isBlank()
        ? parseMonth("month", month)
        : requestedMonth(requested, YearMonth.from(todayDate));
    WeekFields weekFields = weekStartsOn == null
        ? WeekFields.of(locale)
        : WeekFields.of(DayOfWeek.SUNDAY.plus(Math.floorMod(weekStartsOn.intValue(), 7)), firstWeekMinDays());
    DayOfWeek firstDay = weekFields.getFirstDayOfWeek();

    LocalDate first = shown.atDay(1);
    LocalDate start = first.minusDays(Math.floorMod(first.getDayOfWeek().getValue() - firstDay.getValue(), 7));
    long weekCount = (ChronoUnit.DAYS.between(start, shown.atEndOfMonth()) / 7) + 1;
    DateTimeFormatter label = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale);
    List<CalendarWeek> weeks = new ArrayList<>();
    for (int week = 0; week < weekCount; week++) {
      LocalDate weekStart = start.plusWeeks(week);
      List<CalendarDay> days = new ArrayList<>();
      for (int day = 0; day < 7; day++) {
        LocalDate date = weekStart.plusDays(day);
        days.add(new CalendarDay(date, label.format(date), !YearMonth.from(date).equals(shown),
            date.equals(todayDate)));
      }
      weeks.add(new CalendarWeek(weekStart.get(weekFields.weekOfWeekBasedYear()), days));
    }
    String caption = DateTimeFormatter.ofPattern("LLLL y", locale).format(shown);
    return new CalendarMonth(shown, caption, weekdays(firstDay), weeks);
  }

  /**
   * The column headers, as {@code slCalendar} names them: two letters where those stay distinct ({@code Su Mo}), as
   * react-day-picker shows them, otherwise the locale's narrow names, as some languages' short names share a prefix.
   */
  private List<CalendarWeekday> weekdays(DayOfWeek firstDay) {
    List<DayOfWeek> days = new ArrayList<>();
    for (int index = 0; index < 7; index++) {
      days.add(firstDay.plus(index));
    }
    List<String> shortNames = days.stream()
        .map(day -> {
          String name = day.getDisplayName(TextStyle.SHORT_STANDALONE, locale);
          return name.substring(0, Math.min(2, name.length()));
        })
        .toList();
    List<String> names = new HashSet<>(shortNames).size() == 7
        ? shortNames
        : days.stream().map(day -> day.getDisplayName(TextStyle.NARROW_STANDALONE, locale)).toList();
    List<CalendarWeekday> weekdays = new ArrayList<>();
    for (int index = 0; index < 7; index++) {
      weekdays.add(new CalendarWeekday(names.get(index), days.get(index).getDisplayName(TextStyle.FULL_STANDALONE,
          locale)));
    }
    return weekdays;
  }

  private static YearMonth requestedMonth(@Nullable String requested, YearMonth fallback) {
    if (requested == null || requested.isBlank()) {
      return fallback;
    }
    try {
      return toMonth(requested.trim());
    } catch (DateTimeParseException e) {
      return fallback;
    }
  }

  private static YearMonth parseMonth(String prop, String value) {
    try {
      return toMonth(value.trim());
    } catch (DateTimeParseException e) {
      throw new ShadleafComponentException(
          "Invalid value '%s' for prop '%s' of <sl:month-view>: give a month as 2026-10 (a YearMonth) or a date in it."
              .formatted(value, prop), e);
    }
  }

  private static LocalDate parseDate(String prop, String value) {
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      throw new ShadleafComponentException(
          "Invalid value '%s' for prop '%s' of <sl:month-view>: give a date as 2026-10-23 (a LocalDate)."
              .formatted(value, prop), e);
    }
  }

  /** A month ({@code 2026-10}) or a date in it ({@code 2026-10-23}). */
  private static YearMonth toMonth(String value) {
    return value.length() > 7 ? YearMonth.from(LocalDate.parse(value)) : YearMonth.parse(value);
  }

  private static int dayNumber(DayOfWeek day) {
    return day.getValue() % 7;
  }
}
