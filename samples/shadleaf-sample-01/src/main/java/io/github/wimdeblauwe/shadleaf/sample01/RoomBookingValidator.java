package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Checks {@link ValidRoomBooking}. Spring creates it, so it gets the {@link RoomBookings}. Each rule reports on one
 * field, with {@code addPropertyNode}, and only when the fields it needs have a value: their own annotations report
 * those that are missing.
 *
 * <ul>
 *   <li>date: a weekday, today or later</li>
 *   <li>start: within opening hours</li>
 *   <li>end: after the start, at most four hours later, and by closing time (depends on start)</li>
 *   <li>room: a known room, free at that time (depends on date, start and end)</li>
 *   <li>attendees: at most the room's capacity (depends on room)</li>
 *   <li>catering: lunch for four people or more (depends on attendees)</li>
 * </ul>
 */
public class RoomBookingValidator implements ConstraintValidator<ValidRoomBooking, RoomBookingForm> {

  static final Duration LONGEST = Duration.ofHours(4);
  static final int LUNCH_FROM = 4;

  private final RoomBookings roomBookings;

  public RoomBookingValidator(RoomBookings roomBookings) {
    this.roomBookings = roomBookings;
  }

  @Override
  public boolean isValid(RoomBookingForm form, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();
    boolean valid = true;

    LocalDate date = form.getDate();
    if (date != null && date.isBefore(LocalDate.now())) {
      valid = reject(context, "date", "Pick today or a later day.");
    } else if (date != null && (date.getDayOfWeek() == DayOfWeek.SATURDAY
        || date.getDayOfWeek() == DayOfWeek.SUNDAY)) {
      valid = reject(context, "date", "The rooms are closed at the weekend.");
    }

    LocalTime start = form.getStart();
    LocalTime end = form.getEnd();
    if (start != null && (start.isBefore(RoomBookings.OPENS) || !start.isBefore(RoomBookings.CLOSES))) {
      valid = reject(context, "start", "Start between 08:00 and 18:00.");
    }
    boolean timesValid = start != null && end != null;
    if (timesValid && !end.isAfter(start)) {
      valid = timesValid = reject(context, "end", "The meeting must end after it starts.");
    } else if (timesValid && Duration.between(start, end).compareTo(LONGEST) > 0) {
      valid = timesValid = reject(context, "end", "A meeting lasts at most 4 hours.");
    } else if (end != null && end.isAfter(RoomBookings.CLOSES)) {
      valid = timesValid = reject(context, "end", "The rooms close at 18:00.");
    }

    boolean roomChosen = form.getRoom() != null && !form.getRoom().isBlank();
    RoomBookings.Room room = roomChosen ? roomBookings.room(form.getRoom()).orElse(null) : null;
    if (roomChosen && room == null) {
      valid = reject(context, "room", "Choose one of the rooms.");
    } else if (room != null && date != null && timesValid) {
      var taken = roomBookings.overlapping(room.code(), date, start, end);
      if (taken.isPresent()) {
        valid = reject(context, "room", "The %s is booked from %s to %s that day.".formatted(room.name(),
            taken.get().start(), taken.get().end()));
      }
    }

    Integer attendees = form.getAttendees();
    if (room != null && attendees != null && attendees > room.capacity()) {
      valid = reject(context, "attendees", "The %s seats %d people.".formatted(room.name(), room.capacity()));
    }

    if ("lunch".equals(form.getCatering()) && attendees != null && attendees < LUNCH_FROM) {
      valid = reject(context, "catering", "Lunch is for 4 people or more.");
    } else if (form.getCatering() != null && !form.getCatering().matches("none|coffee|lunch")) {
      valid = reject(context, "catering", "Choose one of the options.");
    }
    return valid;
  }

  /** Reports the message on the field; always false, for {@code valid = reject(...)}. */
  private static boolean reject(ConstraintValidatorContext context, String field, String message) {
    context.buildConstraintViolationWithTemplate(message.replace("{", "\\{").replace("$", "\\$"))
        .addPropertyNode(field)
        .addConstraintViolation();
    return false;
  }
}
