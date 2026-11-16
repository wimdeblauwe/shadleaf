package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The room booking page: a form validated on the server as the user types ({@code <sl:form validate>}, htmx 4). Each
 * control posts the whole form to /room-booking/validate while it is edited; that binds and validates it as the submit
 * does, saves nothing, and answers with the form, from which htmx takes the field being edited (and the fields that
 * depend on it). The rules are Bean Validation's: annotations on {@link RoomBookingForm}, and
 * {@link ValidRoomBooking} for those that span fields or need the bookings. The submit validates again: a valid
 * booking is saved and redirects back to an empty form with a toast (post/redirect/get), an invalid one shows the
 * page with its errors. Without JavaScript only the submit validates.
 */
@Controller
public class RoomBookingController {

  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH);

  private final RoomBookings roomBookings;

  public RoomBookingController(RoomBookings roomBookings) {
    this.roomBookings = roomBookings;
  }

  @ModelAttribute("rooms")
  List<RoomBookings.Room> rooms() {
    return roomBookings.rooms();
  }

  @ModelAttribute("today")
  LocalDate today() {
    return LocalDate.now();
  }

  /** The bookings from today on, listed under the form. */
  @ModelAttribute("bookings")
  List<RoomBookings.Booking> bookings() {
    return roomBookings.from(today());
  }

  @GetMapping("/room-booking")
  public String form(@ModelAttribute("roomBooking") RoomBookingForm roomBooking) {
    return "room-booking";
  }

  /**
   * The answer to a validation request: the form again, with its errors. The {@code SL-Validate-Field} header names
   * the field being edited; this controller validates everything regardless, which is cheap here.
   */
  @PostMapping("/room-booking/validate")
  public String validate(@Valid @ModelAttribute("roomBooking") RoomBookingForm roomBooking,
      BindingResult bindingResult) {
    return "room-booking :: booking";
  }

  @PostMapping("/room-booking")
  public String submit(@Valid @ModelAttribute("roomBooking") RoomBookingForm roomBooking, BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return "room-booking";
    }
    roomBookings.add(new RoomBookings.Booking(roomBooking.getRoom(), roomBooking.getDate(), roomBooking.getStart(),
        roomBooking.getEnd(), roomBooking.getTitle()));
    String room = roomBookings.room(roomBooking.getRoom()).orElseThrow().name();
    redirectAttributes.addFlashAttribute("toasts", List.of(Toast.success("Room booked")
        .withDescription("%s, %s from %s to %s.".formatted(room, DAY.format(roomBooking.getDate()),
            roomBooking.getStart(), roomBooking.getEnd()))));
    return "redirect:/room-booking";
  }
}
