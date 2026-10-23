package io.github.wimdeblauwe.shadleaf.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/** {@code #slCalendar}: the first day of the week and the week numbering of a locale, as react-day-picker counts. */
class CalendarLocaleTest {

  @Test
  void theUnitedStatesStartOnSundayAndCountTheWeekOfTheFirstOfJanuaryAsTheFirst() {
    CalendarLocale calendar = new CalendarLocale(Locale.US);

    assertThat(calendar.languageTag()).isEqualTo("en-US");
    assertThat(calendar.weekStartsOn()).isZero();
    assertThat(calendar.firstWeekMinDays()).isEqualTo(1);
  }

  @Test
  void mostOfEuropeCountsIsoWeeks() {
    CalendarLocale calendar = new CalendarLocale(Locale.GERMANY);

    assertThat(calendar.weekStartsOn()).isEqualTo(1);
    assertThat(calendar.firstWeekMinDays()).isEqualTo(4);
  }

  @Test
  void someCountriesStartOnSaturday() {
    assertThat(new CalendarLocale(Locale.forLanguageTag("ar-EG")).weekStartsOn()).isEqualTo(6);
  }
}
