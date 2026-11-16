package io.github.wimdeblauwe.shadleaf.sample01;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * The meeting rooms and their bookings, in memory: a few fixed ones this week and next, and those made on the room
 * booking page. Whether a room is free is only known here, on the server, which is why the page validates as the user
 * types instead of checking in the browser.
 */
@Component
public class RoomBookings {

  static final LocalTime OPENS = LocalTime.of(8, 0);
  static final LocalTime CLOSES = LocalTime.of(18, 0);

  static final List<Room> ROOMS = List.of(
      new Room("atrium", "Atrium", 12),
      new Room("boardroom", "Boardroom", 8),
      new Room("cellar", "Cellar", 4),
      new Room("library", "Library", 2));

  private final List<Booking> bookings = new CopyOnWriteArrayList<>();

  public RoomBookings() {
    LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    for (LocalDate week : List.of(monday, monday.plusWeeks(1))) {
      bookings.add(new Booking("atrium", week.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), "Team meeting"));
      bookings.add(new Booking("boardroom", week.plusDays(2), LocalTime.of(9, 0), LocalTime.of(11, 30), "Board"));
      bookings.add(new Booking("cellar", week.plusDays(3), LocalTime.of(14, 0), LocalTime.of(16, 0), "Retrospective"));
      bookings.add(new Booking("library", week.plusDays(4), LocalTime.of(13, 0), LocalTime.of(14, 0), "One-on-one"));
    }
  }

  public List<Room> rooms() {
    return ROOMS;
  }

  public Optional<Room> room(String code) {
    return ROOMS.stream().filter(room -> room.code().equals(code)).findFirst();
  }

  /** The bookings from the given day on, in date and time order: the page lists them. */
  public List<Booking> from(LocalDate day) {
    return bookings.stream()
        .filter(booking -> !booking.date().isBefore(day))
        .sorted((a, b) -> a.date().equals(b.date()) ? a.start().compareTo(b.start()) : a.date().compareTo(b.date()))
        .toList();
  }

  /** The booking of the room that overlaps the given time on that day, if any. */
  public Optional<Booking> overlapping(String room, LocalDate date, LocalTime start, LocalTime end) {
    return bookings.stream()
        .filter(booking -> booking.room().equals(room) && booking.date().equals(date))
        .filter(booking -> booking.start().isBefore(end) && start.isBefore(booking.end()))
        .findFirst();
  }

  public void add(Booking booking) {
    bookings.add(booking);
  }

  public record Room(String code, String name, int capacity) {
  }

  public record Booking(String room, LocalDate date, LocalTime start, LocalTime end, String title) {
  }
}
