package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * The form backing object of the room booking page. The rules on one field are annotations here; the rules that span
 * several fields, and the one that needs the bookings, are in {@link ValidRoomBooking}, which reports each on the field
 * that shows it.
 */
@ValidRoomBooking
public class RoomBookingForm {

  @NotBlank(message = "Give the meeting a title.")
  @Size(max = 60, message = "Keep the title to 60 characters.")
  private String title;

  /** A room's code, from {@link RoomBookings#rooms()}. */
  @NotBlank(message = "Choose a room.")
  private String room;

  /** ISO, as the date picker's input submits it. */
  @NotNull(message = "Pick a day.")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  /** Hours and minutes, as a time input submits them. */
  @NotNull(message = "Enter when the meeting starts.")
  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime start;

  @NotNull(message = "Enter when the meeting ends.")
  @DateTimeFormat(pattern = "HH:mm")
  private LocalTime end;

  @NotNull(message = "Enter how many people attend.")
  @Min(value = 1, message = "At least one person attends.")
  private Integer attendees;

  /** none, coffee or lunch: a radio group in a field set. */
  private String catering = "none";

  @NotBlank(message = "Enter your email address.")
  @Email(message = "Enter a valid email address.")
  private String organiserEmail;

  @Size(max = 200, message = "Keep the notes to 200 characters.")
  private String notes;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalTime getStart() {
    return start;
  }

  public void setStart(LocalTime start) {
    this.start = start;
  }

  public LocalTime getEnd() {
    return end;
  }

  public void setEnd(LocalTime end) {
    this.end = end;
  }

  public Integer getAttendees() {
    return attendees;
  }

  public void setAttendees(Integer attendees) {
    this.attendees = attendees;
  }

  public String getCatering() {
    return catering;
  }

  public void setCatering(String catering) {
    this.catering = catering;
  }

  public String getOrganiserEmail() {
    return organiserEmail;
  }

  public void setOrganiserEmail(String organiserEmail) {
    this.organiserEmail = organiserEmail;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
