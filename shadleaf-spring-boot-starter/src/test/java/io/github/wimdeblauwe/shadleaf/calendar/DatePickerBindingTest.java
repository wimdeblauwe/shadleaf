package io.github.wimdeblauwe.shadleaf.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.wimdeblauwe.shadleaf.calendar.CalendarBindingTest.Booking;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * {@code <sl:date-picker>} in a {@code th:object} form: the calendar inside binds its input (or keeps a range's
 * inputs), and the button shows the bound date, formatted on the server in the page's locale, before
 * {@code slDatePicker} formats it again.
 */
class DatePickerBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void theButtonOpensThePopoverHoldingTheCalendar() {
    Rendered rendered = render(renderer, new Booking(null, null), Map.of(), """
        <sl:date-picker th:field="*{arrival}" aria-label="Arrival" class="w-full" popover-id="arrival-picker"/>""");

    Element picker = rendered.select(".date-picker").first();
    Element trigger = picker.selectFirst("> button");
    Element content = picker.selectFirst("> .popover-content");
    assertThat(picker.classNames()).containsExactly("date-picker", "w-full");
    assertThat(picker.attr("x-data")).isEqualTo("slDatePicker");
    assertThat(trigger.classNames()).contains("button", "popover-trigger", "date-picker-trigger");
    assertThat(trigger.attr("data-variant")).isEqualTo("outline");
    assertThat(trigger.attr("type")).isEqualTo("button");
    assertThat(trigger.attr("popovertarget")).isEqualTo("arrival-picker");
    assertThat(trigger.attr("aria-haspopup")).isEqualTo("dialog");
    assertThat(trigger.select("svg")).as("the calendar icon").hasSize(1);
    assertThat(content.id()).isEqualTo("arrival-picker");
    assertThat(content.classNames()).contains("popover-content", "date-picker-content");
    assertThat(content.hasAttr("popover")).isTrue();
    assertThat(content.attr("role")).isEqualTo("dialog");
    assertThat(content.attr("aria-label")).isEqualTo("Choose a date");
    assertThat(content.attr("data-align")).isEqualTo("start");
    assertThat(content.select("> .calendar")).hasSize(1);
  }

  @Test
  void withoutADateTheButtonShowsThePlaceholder() {
    Element trigger = render(renderer, new Booking(null, null), Map.of(),
        "<sl:date-picker th:field=\"*{arrival}\" aria-label=\"Arrival\"/>").select(".date-picker-trigger").first();
    Element custom = render(renderer, new Booking(null, null), Map.of(),
        "<sl:date-picker th:field=\"*{arrival}\" aria-label=\"Arrival\" placeholder=\"When do you arrive?\"/>")
        .select(".date-picker-trigger").first();

    assertThat(trigger.attr("data-empty")).isEqualTo("true");
    assertThat(trigger.selectFirst(".date-picker-value").text()).isEqualTo("Pick a date");
    assertThat(custom.selectFirst(".date-picker-value").text()).isEqualTo("When do you arrive?");
  }

  @Test
  void theCalendarBindsItsInputAndTheButtonShowsTheDate() {
    Rendered rendered = render(renderer, new Booking(LocalDate.of(2026, 10, 23), null), Map.of(), """
        <sl:date-picker th:field="*{arrival}" aria-label="Arrival" required min="2026-01-01" caption-layout="dropdown"/>""");

    Element picker = rendered.select(".date-picker").first();
    Element calendar = picker.selectFirst(".calendar");
    Element input = calendar.selectFirst("> input");
    Element trigger = picker.selectFirst(".date-picker-trigger");
    assertThat(trigger.hasAttr("data-empty")).isFalse();
    assertThat(trigger.selectFirst(".date-picker-value").text()).isEqualTo("October 23, 2026");
    assertThat(input.id()).isEqualTo("arrival");
    assertThat(input.attr("name")).isEqualTo("arrival");
    assertThat(input.val()).isEqualTo("2026-10-23");
    assertThat(input.attr("aria-label")).isEqualTo("Arrival");
    assertThat(input.hasAttr("required")).isTrue();
    assertThat(input.attr("min")).isEqualTo("2026-01-01");
    assertThat(calendar.attr("data-caption")).as("a calendar prop reaches the calendar").isEqualTo("dropdown");
    assertThat(picker.hasAttr("aria-label")).isFalse();
  }

  @Test
  void inAFieldTheInputTakesTheFieldsIdDescriptionAndErrors() {
    Rendered rendered = render(renderer, new Booking(null, null), Map.of("arrival", List.of("must not be null")), """
        <sl:field th:field="*{arrival}">
          <sl:field-label>Arrival</sl:field-label>
          <sl:date-picker/>
          <sl:field-description>The day you arrive.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element input = rendered.select(".calendar > input").first();
    assertThat(input.id()).isEqualTo("arrival");
    assertThat(input.attr("aria-invalid")).isEqualTo("true");
    assertThat(input.attr("aria-describedby")).isEqualTo("arrival-description arrival-error");
    assertThat(rendered.select("label.field-label").first().attr("for")).isEqualTo("arrival");
    assertThat(rendered.select(".date-picker-trigger").first().attr("data-empty")).isEqualTo("true");
  }

  @Test
  void aBoundDateInAFieldIsShown() {
    Element trigger = render(renderer, new Booking(LocalDate.of(2026, 3, 5), null), Map.of(), """
        <sl:field th:field="*{arrival}">
          <sl:field-label>Arrival</sl:field-label>
          <sl:date-picker format="medium"/>
        </sl:field>""").select(".date-picker-trigger").first();

    assertThat(trigger.selectFirst(".date-picker-value").text()).isEqualTo("Mar 5, 2026");
  }

  @Test
  void aRangeShowsBothEndsAndItsInputsStayInTheCalendar() {
    Rendered rendered = render(renderer, new Booking(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16)), Map.of(),
        """
            <sl:date-picker aria-label="Stay" number-of-months="2">
              <sl:input type="date" th:field="*{arrival}" aria-label="Arrival"/>
              <sl:input type="date" th:field="*{departure}" aria-label="Departure"/>
            </sl:date-picker>""");

    Element calendar = rendered.select(".calendar").first();
    assertThat(calendar.attr("aria-label")).isEqualTo("Stay");
    assertThat(calendar.attr("data-number-of-months")).isEqualTo("2");
    assertThat(calendar.select("> input")).extracting(Element::id, Element::val)
        .containsExactly(tuple("arrival", "2026-10-12"), tuple("departure", "2026-10-16"));
    assertThat(rendered.select(".date-picker-value").text()).isEqualTo("October 12, 2026 – October 16, 2026");
  }

  @Test
  void aRangeWithOnlyItsStartShowsTheStart() {
    Element value = render(renderer, new Booking(LocalDate.of(2026, 10, 12), null), Map.of(), """
        <sl:date-picker aria-label="Stay">
          <sl:input type="date" th:field="*{arrival}" aria-label="Arrival"/>
          <sl:input type="date" th:field="*{departure}" aria-label="Departure"/>
        </sl:date-picker>""").select(".date-picker-value").first();

    assertThat(value.text()).isEqualTo("October 12, 2026");
  }

  @Test
  void valuesWithoutABindingAreShownToo() {
    Map<String, Object> variables = Map.of("start", LocalDate.of(2026, 1, 2));
    Rendered rendered = renderer.render("""
        <sl:date-picker name="single" value="2026-10-23" aria-label="Single"/>
        <sl:date-picker aria-label="Range">
          <sl:input type="date" name="from" th:value="${start}" aria-label="From"/>
          <sl:input type="date" name="to" value="2026-01-09" aria-label="To"/>
        </sl:date-picker>
        <sl:date-picker name="broken" value="23/10/2026" aria-label="Not an ISO date"/>""", variables);

    assertThat(rendered.select(".date-picker-value")).extracting(Element::text)
        .containsExactly("October 23, 2026", "January 2, 2026 – January 9, 2026", "Pick a date");
  }

  @Test
  void thePagesLocaleFormatsTheDate() {
    ComponentRenderTester belgian = ComponentRenderTester.builder().locale(Locale.forLanguageTag("nl-BE")).build();
    Rendered rendered = render(belgian, new Booking(LocalDate.of(2026, 10, 23), null), Map.of(), """
        <sl:date-picker th:field="*{arrival}" aria-label="Aankomst"/>
        <sl:date-picker th:field="*{arrival}" aria-label="Aankomst" format="full"/>""");

    assertThat(rendered.select(".date-picker-value")).extracting(Element::text)
        .containsExactly("23 oktober 2026", "vrijdag 23 oktober 2026");
    assertThat(rendered.select(".calendar").first().attr("data-locale")).isEqualTo("nl-BE");
  }

  @Test
  void theFormatAndTheTextsReachTheScript() {
    Element plain = render(renderer, new Booking(null, null), Map.of(), "<sl:date-picker th:field=\"*{arrival}\"/>")
        .select(".date-picker").first();
    Element configured = render(renderer, new Booking(null, null), Map.of(),
        "<sl:date-picker th:field=\"*{arrival}\" format=\"short\" placeholder=\"Pick one\"/>")
        .select(".date-picker").first();

    assertThat(plain.hasAttr("data-date-style")).as("long is the default").isFalse();
    assertThat(plain.attr("data-placeholder")).isEqualTo("Pick a date");
    assertThat(plain.attr("data-range-format")).isEqualTo("{0} – {1}");
    assertThat(configured.attr("data-date-style")).isEqualTo("short");
    assertThat(configured.attr("data-placeholder")).isEqualTo("Pick one");
  }

  @Test
  void aDisabledDateDisablesTheButton() {
    Element trigger = render(renderer, new Booking(null, null), Map.of(),
        "<sl:date-picker th:field=\"*{arrival}\" aria-label=\"Arrival\" disabled/>").select(".date-picker-trigger")
        .first();

    assertThat(trigger.hasAttr("disabled")).isTrue();
  }

  @Test
  void withoutAPopoverIdEachRenderGeneratesOne() {
    Rendered rendered = render(renderer, new Booking(null, null), Map.of(), """
        <sl:date-picker th:field="*{arrival}"/>
        <sl:date-picker th:field="*{departure}"/>""");

    List<String> ids = rendered.select(".date-picker-content").eachAttr("id");
    assertThat(ids).hasSize(2).doesNotHaveDuplicates().allMatch(id -> id.matches("sl-date-picker-[a-z0-9]{8}"));
    assertThat(rendered.select(".date-picker-trigger").eachAttr("popovertarget")).isEqualTo(ids);
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
}
