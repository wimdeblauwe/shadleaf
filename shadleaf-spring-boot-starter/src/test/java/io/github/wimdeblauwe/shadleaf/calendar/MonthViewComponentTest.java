package io.github.wimdeblauwe.shadleaf.calendar;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:month-view>}: the weeks of a month in the page's locale, a day slot rendered once per day with
 * {@code slDay}, days as links, and the previous and next month as links to the same page.
 */
class MonthViewComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void rendersTheWeeksOfTheMonthFromTheFirstDayOfTheLocalesWeek() {
    Rendered rendered = tester.render("<sl:month-view month=\"2026-10\" today=\"2026-10-23\"/>");

    assertThat(rendered.select("caption").text()).isEqualTo("October 2026");
    assertThat(rendered.select(".month-view-weekday").eachText())
        .containsExactly("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa");
    assertThat(rendered.select(".month-view-weekday").first().attr("aria-label")).isEqualTo("Sunday");
    Elements weeks = rendered.select(".month-view-week");
    assertThat(weeks).hasSize(5);
    assertThat(weeks.first().select(".month-view-day-number").eachText())
        .containsExactly("27", "28", "29", "30", "1", "2", "3");
    assertThat(weeks.first().select(".month-view-day[data-outside]")).hasSize(4);
    Element today = rendered.select(".month-view-day[data-today]").first();
    assertThat(today.select(".month-view-day-number").text()).isEqualTo("23");
    assertThat(today.select(".month-view-day-body").attr("aria-current")).isEqualTo("date");
    assertThat(today.select(".sl-sr-only").text()).isEqualTo("Friday, October 23, 2026");
    assertThat(rendered.select(".month-view-day-link")).isEmpty();
    assertThat(rendered.select(".month-view-week-number")).isEmpty();
  }

  @Test
  void aYearMonthOrADateInTheMonthWorkToo() {
    Rendered fromYearMonth = tester.render("<sl:month-view th:month=\"${month}\"/>",
        Map.of("month", YearMonth.of(2027, 2)));
    Rendered fromDate = tester.render("<sl:month-view th:month=\"${date}\"/>",
        Map.of("date", LocalDate.of(2027, 2, 14)));

    assertThat(fromYearMonth.select("caption").text()).isEqualTo("February 2027");
    assertThat(fromDate.select("caption").text()).isEqualTo("February 2027");
  }

  @Test
  void weekNumbersAndTheFirstDayOfTheWeekFollowTheLocaleOrWeekStartsOn() {
    Rendered belgian = ComponentRenderTester.builder().locale(Locale.forLanguageTag("nl-BE")).build()
        .render("<sl:month-view month=\"2026-01\" show-week-number/>");
    Rendered sunday = ComponentRenderTester.builder().locale(Locale.forLanguageTag("nl-BE")).build()
        .render("<sl:month-view month=\"2026-01\" show-week-number week-starts-on=\"0\"/>");

    assertThat(belgian.select("caption").text()).isEqualTo("januari 2026");
    assertThat(belgian.select(".month-view-weekday").eachText())
        .containsExactly("ma", "di", "wo", "do", "vr", "za", "zo");
    assertThat(belgian.select(".month-view-week-number").eachText()).containsExactly("1", "2", "3", "4", "5");
    assertThat(belgian.select(".month-view-week-number").first().attr("aria-label")).isEqualTo("Week 1");
    assertThat(belgian.select(".month-view-week-number-header")).hasSize(1);
    assertThat(sunday.select(".month-view-weekday").first().text()).isEqualTo("zo");
    assertThat(sunday.select(".month-view-week").first().select(".month-view-day-number").first().text())
        .isEqualTo("28");
  }

  @Test
  void theDaySlotIsRenderedForEveryDayAndSeesTheDayAndTheModel() {
    Rendered rendered = tester.render("""
        <sl:month-view month="2026-10">
          <sl:slot name="day"><span class="count" th:if="${counts[slDay.date]}"
              th:text="${counts[slDay.date]}"></span></sl:slot>
        </sl:month-view>""", Map.of("counts", Map.of(LocalDate.of(2026, 10, 12), 3, LocalDate.of(2026, 10, 30), 1)));

    assertThat(rendered.select(".month-view-day-content")).hasSize(35);
    Elements counts = rendered.select(".count");
    assertThat(counts.eachText()).containsExactly("3", "1");
    assertThat(counts.first().closest(".month-view-day").select(".month-view-day-number").text()).isEqualTo("12");
  }

  @Test
  void dayHrefMakesEveryDayALinkWithTheDateAndTheContextPath() {
    Rendered rendered = ComponentRenderTester.builder().contextPath("/app").build().render("""
        <sl:month-view month="2026-10" today="2026-10-23" day-href="/agenda/{date}">
          <sl:slot name="day">Busy</sl:slot>
        </sl:month-view>""");

    Elements links = rendered.select("a.month-view-day-link");
    assertThat(links).hasSize(35);
    Element today = rendered.select(".month-view-day[data-today] > a").first();
    assertThat(today.attr("href")).isEqualTo("/app/agenda/2026-10-23");
    assertThat(today.attr("aria-current")).isEqualTo("date");
    assertThat(today.select("> .month-view-day-number").attr("aria-hidden")).isEqualTo("true");
    assertThat(today.select("> .sl-sr-only, > .month-view-day-content").eachText())
        .as("the link's name: the whole date and the day's content")
        .containsExactly("Friday, October 23, 2026", "Busy");
    assertThat(links.first().attr("href")).isEqualTo("/app/agenda/2026-09-27");
    assertThat(links.first().hasAttr("aria-current")).isFalse();
  }

  @Test
  void outsideDaysCanBeLeftEmpty() {
    Rendered rendered = tester.render("""
        <sl:month-view month="2026-10" show-outside-days="false" day-href="?date={date}"/>""");

    assertThat(rendered.select(".month-view-day[data-hidden]")).as("27 to 30 September").hasSize(4);
    assertThat(rendered.select(".month-view-day[data-hidden]").first().children()).isEmpty();
    assertThat(rendered.select("a.month-view-day-link")).hasSize(31);
  }

  @Test
  void theMonthComesFromTheRequestAndItsLinksKeepTheOtherParameters() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/agenda?team=ops&month=2026-11").build()
        .render("<sl:month-view/>");

    assertThat(rendered.select("caption").text()).isEqualTo("November 2026");
    assertThat(rendered.select("a.month-view-previous").attr("href")).isEqualTo("/agenda?team=ops&month=2026-10");
    assertThat(rendered.select("a.month-view-next").attr("href")).isEqualTo("/agenda?team=ops&month=2026-12");
    assertThat(rendered.select("a.month-view-previous").attr("aria-label")).isEqualTo("Go to the previous month");
  }

  @Test
  void monthParamNamesTheParameter() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/rota?month=2026-01&rota=2026-03").build()
        .render("<sl:month-view month-param=\"rota\"/>");

    assertThat(rendered.select("caption").text()).isEqualTo("March 2026");
    assertThat(rendered.select("a.month-view-next").attr("href")).isEqualTo("/rota?month=2026-01&rota=2026-04");
  }

  @Test
  void aRequestedMonthThatIsNoMonthShowsTheCurrentOne() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/agenda?month=soon").build()
        .render("<sl:month-view today=\"2026-10-23\"/>");

    assertThat(rendered.select("caption").text()).isEqualTo("October 2026");
  }

  @Test
  void aMonthPropThatIsNoMonthFails() {
    assertThatRenderFailure(() -> tester.render("<sl:month-view month=\"October\"/>"))
        .hasMessageContaining("Invalid value 'October' for prop 'month' of <sl:month-view>");
  }

  @Test
  void callerAttributesGoOnTheRoot() {
    Rendered rendered = tester.render("<sl:month-view month=\"2026-10\" id=\"agenda\" class=\"w-full\"/>");

    Element root = rendered.select(".month-view").first();
    assertThat(root.id()).isEqualTo("agenda");
    assertThat(root.classNames()).containsExactly("month-view", "w-full");
  }
}
