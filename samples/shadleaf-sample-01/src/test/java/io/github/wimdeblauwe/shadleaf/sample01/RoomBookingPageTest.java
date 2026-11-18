package io.github.wimdeblauwe.shadleaf.sample01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The room booking page: a form validated as the user types ({@code sl:form validate}), with fields that depend on
 * others ({@code depends-on}). The validation endpoint answers with the form; a field that depends on the one being
 * validated is wrapped in an {@code <hx-partial>} once it has a value.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoomBookingPageTest {

  /** A weekday at least a week away, so it is never in the past and the fixed bookings are known. */
  private static final LocalDate MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private RoomBookings roomBookings;

  @Test
  void everyControlValidatesAsTheUserTypes() throws Exception {
    Document page = page(get("/room-booking"));

    Element title = page.getElementById("title");
    assertThat(title.attr("hx-post")).isEqualTo("/room-booking/validate");
    assertThat(title.attr("hx-trigger")).isEqualTo("input changed delay:200ms");
    assertThat(title.attr("hx-target")).isEqualTo("closest .field");
    assertThat(title.attr("hx-select")).isEqualTo("[data-validate-field='title']");
    assertThat(title.attr("hx-headers")).isEqualTo("SL-Validate-Field:'title'");
    assertThat(page.getElementById("start").attr("hx-trigger")).isEqualTo("change");
    assertThat(page.getElementById("room").attr("hx-trigger")).isEqualTo("change");
    assertThat(page.getElementById("date").attr("hx-post")).isEqualTo("/room-booking/validate");
    assertThat(page.select("input[name=catering]")).hasSize(3)
        .allMatch(radio -> radio.attr("hx-target").equals("closest .field-set"));
    assertThat(page.select("[data-validate-field]")).extracting(element -> element.attr("data-validate-field"))
        .containsExactly("title", "date", "start", "end", "room", "attendees", "catering", "organiserEmail", "notes");
    assertThat(page.select("hx-partial")).as("only in the answer to a validation request").isEmpty();
    assertThat(page.select("[aria-invalid], .form-errors")).isEmpty();
  }

  @Test
  void theValidationAnswerIsTheFormAloneWithItsErrors() throws Exception {
    Document answer = page(validate("title").param("title", ""));

    assertThat(answer.select("form.form")).hasSize(1);
    assertThat(answer.select("main, nav, table")).as("the form alone").isEmpty();
    assertThat(answer.getElementById("title-error").text()).isEqualTo("Give the meeting a title.");
    assertThat(answer.select(".form-errors[autofocus], [autofocus]")).as("no focus taken while typing").isEmpty();
  }

  @Test
  void theEndDependsOnTheStartOnceItHasAValue() throws Exception {
    Document filled = page(validate("start").param("start", "10:00").param("end", "09:30"));
    Document empty = page(validate("start").param("start", "10:00").param("end", ""));

    Element partial = filled.selectFirst("hx-partial:has([data-validate-field=end])");
    assertThat(partial).isNotNull();
    assertThat(partial.attr("hx-target")).isEqualTo("[data-validate-field='end']");
    assertThat(filled.getElementById("end-error").text()).isEqualTo("The meeting must end after it starts.");
    assertThat(empty.select("hx-partial")).isEmpty();
  }

  @Test
  void theEndIsAtMostFourHoursAfterTheStartAndByClosingTime() throws Exception {
    assertThat(page(validate("end").param("start", "09:00").param("end", "13:30")).getElementById("end-error").text())
        .isEqualTo("A meeting lasts at most 4 hours.");
    assertThat(page(validate("end").param("start", "17:00").param("end", "18:30")).getElementById("end-error").text())
        .isEqualTo("The rooms close at 18:00.");
    assertThat(page(validate("start").param("start", "07:30")).getElementById("start-error").text())
        .isEqualTo("Start between 08:00 and 18:00.");
  }

  @Test
  void theDayIsAWeekdayFromTodayOn() throws Exception {
    assertThat(page(validate("date").param("date", MONDAY.minusDays(1).toString())).getElementById("date-error")
        .text()).isEqualTo("The rooms are closed at the weekend.");
    assertThat(page(validate("date").param("date", LocalDate.now().minusDays(1).toString()))
        .getElementById("date-error").text()).isEqualTo("Pick today or a later day.");
  }

  @Test
  void aRoomBookedAtThatTimeIsRejectedWhenTheTimeChanges() throws Exception {
    RoomBookings.Booking booked = roomBookings.from(MONDAY).get(0);

    Document answer = page(validate("start")
        .param("room", booked.room())
        .param("date", booked.date().toString())
        .param("start", booked.start().toString())
        .param("end", booked.start().plusMinutes(30).toString()));

    assertThat(answer.selectFirst("hx-partial:has([data-validate-field=room])")).isNotNull();
    assertThat(answer.getElementById("room-error").text()).startsWith("The ").contains(" is booked from "
        + booked.start() + " to " + booked.end());
  }

  @Test
  void theAttendeesDependOnTheRoomAndTheCateringOnTheAttendees() throws Exception {
    Document byRoom = page(validate("room").param("room", "library").param("attendees", "6"));
    Document byAttendees = page(validate("attendees").param("attendees", "3").param("catering", "lunch"));

    assertThat(byRoom.selectFirst("hx-partial:has([data-validate-field=attendees])")).isNotNull();
    assertThat(byRoom.getElementById("attendees-error").text()).isEqualTo("The Library seats 2 people.");
    assertThat(byAttendees.selectFirst("hx-partial:has([data-validate-field=catering])")).isNotNull();
    assertThat(byAttendees.getElementById("catering-error").text()).isEqualTo("Lunch is for 4 people or more.");
  }

  @Test
  void anInvalidSubmitShowsThePageWithItsErrors() throws Exception {
    Document page = page(post("/room-booking").param("title", "Planning").param("catering", "none"));

    assertThat(page.select(".form-errors")).as("only field errors: no summary").isEmpty();
    assertThat(page.select("[autofocus]")).as("the first field with an error").hasSize(1);
    assertThat(page.getElementById("title").val()).isEqualTo("Planning");
    assertThat(page.getElementById("room").attr("aria-invalid")).isEqualTo("true");
    assertThat(page.getElementById("organiserEmail-error").text()).isEqualTo("Enter your email address.");
    assertThat(page.select("table.table")).as("the whole page").hasSize(1);
  }

  @Test
  void aValidBookingIsSavedAndRedirectsWithAToast() throws Exception {
    LocalDate day = MONDAY.plusWeeks(3);

    mockMvc.perform(post("/room-booking")
            .param("title", "Planning")
            .param("room", "cellar")
            .param("date", day.toString())
            .param("start", "09:00")
            .param("end", "10:30")
            .param("attendees", "4")
            .param("catering", "lunch")
            .param("organiserEmail", "ada@example.com"))
        .andExpect(redirectedUrl("/room-booking"))
        .andExpect(flash().attribute("toasts", List.of(Toast.success("Room booked")
            .withDescription("Cellar, Monday %d %s from 09:00 to 10:30.".formatted(day.getDayOfMonth(),
                day.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH))))));

    assertThat(page(validate("room").param("room", "cellar").param("date", day.toString())
        .param("start", "10:00").param("end", "11:00")).getElementById("room-error").text())
        .isEqualTo("The Cellar is booked from 09:00 to 10:30 that day.");
  }

  private MockHttpServletRequestBuilder validate(String field) {
    return post("/room-booking/validate").header("HX-Request", "true").header("SL-Validate-Field", field);
  }

  private Document page(MockHttpServletRequestBuilder request) throws Exception {
    return Jsoup.parse(mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
        .getContentAsString());
  }
}
