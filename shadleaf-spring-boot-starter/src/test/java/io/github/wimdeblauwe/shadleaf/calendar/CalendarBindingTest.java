package io.github.wimdeblauwe.shadleaf.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * {@code <sl:calendar>} in a {@code th:object} form: without content it renders one date input that takes
 * {@code th:field} and the other attributes (not {@code class}), as {@code sl:input} does, also from a {@code sl:field};
 * with two date inputs as content (a range) the attributes stay on the calendar. The locale of the page decides the
 * first day of the week and the week numbering, unless {@code week-starts-on} is given.
 */
class CalendarBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void thFieldBindsItsOwnDateInputAndClassStaysOnTheCalendar() {
    Rendered rendered = render(renderer, new Booking(LocalDate.of(2026, 10, 23), null), Map.of(), """
        <sl:calendar th:field="*{arrival}" class="w-full" aria-label="Arrival" required min="2026-01-01"/>""");

    Element calendar = rendered.select(".calendar").first();
    Element input = calendar.selectFirst("> input");
    assertThat(calendar.classNames()).containsExactly("calendar", "w-full");
    assertThat(calendar.attr("x-data")).isEqualTo("slCalendar");
    assertThat(calendar.hasAttr("aria-label")).isFalse();
    assertThat(input.attr("type")).isEqualTo("date");
    assertThat(input.classNames()).containsExactly("input", "calendar-input");
    assertThat(input.id()).isEqualTo("arrival");
    assertThat(input.attr("name")).isEqualTo("arrival");
    assertThat(input.val()).as("an ISO date, what a date input takes").isEqualTo("2026-10-23");
    assertThat(input.attr("aria-label")).isEqualTo("Arrival");
    assertThat(input.hasAttr("required")).isTrue();
    assertThat(input.attr("min")).isEqualTo("2026-01-01");
    assertThat(input.hasAttr("aria-invalid")).isFalse();
  }

  @Test
  void inAFieldItTakesTheFieldsIdDescriptionAndErrors() {
    Rendered rendered = render(renderer, new Booking(null, null), Map.of("arrival", List.of("must not be null")), """
        <sl:field th:field="*{arrival}">
          <sl:field-label>Arrival</sl:field-label>
          <sl:calendar/>
          <sl:field-description>The day you arrive.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element input = rendered.select(".calendar > input").first();
    assertThat(input.id()).isEqualTo("arrival");
    assertThat(input.attr("name")).isEqualTo("arrival");
    assertThat(input.val()).isEmpty();
    assertThat(input.attr("aria-invalid")).isEqualTo("true");
    assertThat(input.attr("aria-describedby")).isEqualTo("arrival-description arrival-error");
    assertThat(rendered.select("label.field-label").first().attr("for")).isEqualTo("arrival");
  }

  @Test
  void aRangeKeepsItsInputsAndTakesTheAttributesItself() {
    Rendered rendered = render(renderer, new Booking(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16)), Map.of(),
        """
            <sl:calendar aria-label="Stay" class="w-full">
              <sl:input type="date" th:field="*{arrival}" aria-label="Arrival"/>
              <sl:input type="date" th:field="*{departure}" aria-label="Departure"/>
            </sl:calendar>""");

    Element calendar = rendered.select(".calendar").first();
    assertThat(calendar.classNames()).containsExactly("calendar", "w-full");
    assertThat(calendar.attr("aria-label")).isEqualTo("Stay");
    assertThat(calendar.select("> input")).extracting(Element::id, Element::val)
        .containsExactly(tuple("arrival", "2026-10-12"),
            tuple("departure", "2026-10-16"));
    assertThat(calendar.select("> input.calendar-input")).as("no input of its own").isEmpty();
  }

  @Test
  void theNavigationAndTheSelectsAreNamedByMessages() {
    Element template = render(renderer, new Booking(null, null), Map.of(), "<sl:calendar th:field=\"*{arrival}\"/>")
        .select(".calendar > template.calendar-template").first();

    assertThat(template.selectFirst(".calendar-previous").attr("aria-label")).isEqualTo("Go to the previous month");
    assertThat(template.selectFirst(".calendar-next").attr("aria-label")).isEqualTo("Go to the next month");
    assertThat(template.select(".calendar-select")).extracting(element -> element.attr("aria-label"))
        .containsExactly("Month", "Year");
    assertThat(template.selectFirst(".calendar-week-number-label").text()).isEqualTo("Week");
    assertThat(template.select("svg")).hasSize(4);
  }

  @Test
  void theLocaleDecidesTheFirstDayOfTheWeekAndTheWeekNumbering() {
    Element us = render(ComponentRenderTester.builder().locale(Locale.US).build(), new Booking(null, null), Map.of(),
        "<sl:calendar th:field=\"*{arrival}\"/>").select(".calendar").first();
    Element belgium = render(ComponentRenderTester.builder().locale(Locale.forLanguageTag("nl-BE")).build(),
        new Booking(null, null), Map.of(), "<sl:calendar th:field=\"*{arrival}\"/>").select(".calendar").first();

    assertThat(us.attr("data-locale")).isEqualTo("en-US");
    assertThat(us.attr("data-week-starts-on")).isEqualTo("0");
    assertThat(us.attr("data-first-week-min-days")).isEqualTo("1");
    assertThat(belgium.attr("data-locale")).isEqualTo("nl-BE");
    assertThat(belgium.attr("data-week-starts-on")).isEqualTo("1");
    assertThat(belgium.attr("data-first-week-min-days")).isEqualTo("4");
  }

  @Test
  void weekStartsOnWinsOverTheLocaleEvenWhenItIsSunday() {
    Element calendar = render(ComponentRenderTester.builder().locale(Locale.forLanguageTag("nl-BE")).build(),
        new Booking(null, null), Map.of(), "<sl:calendar th:field=\"*{arrival}\" week-starts-on=\"0\"/>")
        .select(".calendar").first();

    assertThat(calendar.attr("data-week-starts-on")).isEqualTo("0");
  }

  @Test
  void onlyTheValuesThatDifferFromTheDefaultsAreRendered() {
    Element plain = render(renderer, new Booking(null, null), Map.of(), "<sl:calendar th:field=\"*{arrival}\"/>")
        .select(".calendar").first();
    Element configured = render(renderer, new Booking(null, null), Map.of(), """
        <sl:calendar th:field="*{arrival}" caption-layout="dropdown" number-of-months="2" show-outside-days="false"
                     show-week-number default-month="2026-09" start-month="2026-01" end-month="2026-12"
                     disabled-dates="sat,sun"/>""").select(".calendar").first();

    assertThat(plain.attributes().asList()).extracting(org.jsoup.nodes.Attribute::getKey)
        .containsExactlyInAnyOrder("class", "x-data", "role", "data-locale", "data-week-starts-on",
            "data-first-week-min-days");
    assertThat(configured.attr("data-caption")).isEqualTo("dropdown");
    assertThat(configured.attr("data-number-of-months")).isEqualTo("2");
    assertThat(configured.attr("data-show-outside-days")).isEqualTo("false");
    assertThat(configured.attr("data-show-week-number")).isEqualTo("true");
    assertThat(configured.attr("data-default-month")).isEqualTo("2026-09");
    assertThat(configured.attr("data-start-month")).isEqualTo("2026-01");
    assertThat(configured.attr("data-end-month")).isEqualTo("2026-12");
    assertThat(configured.attr("data-disabled-dates")).isEqualTo("sat,sun");
  }

  /** Every entry in {@code errors} is rejected on the form object's binding, one message at a time. */
  private static Rendered render(ComponentRenderTester renderer, Booking booking, Map<String, List<String>> errors,
      String fields) {
    BindingResult bindingResult = new BeanPropertyBindingResult(booking, "booking");
    errors.forEach((field, messages) -> messages.forEach(message ->
        bindingResult.rejectValue(field, "Invalid", message)));
    Map<String, Object> variables = new HashMap<>();
    variables.put("booking", booking);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "booking", bindingResult);
    return renderer.render("<form th:object=\"${booking}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Booking {

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate arrival;
    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate departure;

    public Booking(LocalDate arrival, LocalDate departure) {
      this.arrival = arrival;
      this.departure = departure;
    }

    public LocalDate getArrival() {
      return arrival;
    }

    public void setArrival(LocalDate arrival) {
      this.arrival = arrival;
    }

    public LocalDate getDeparture() {
      return departure;
    }

    public void setDeparture(LocalDate departure) {
      this.departure = departure;
    }
  }
}
