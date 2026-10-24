package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A server-side validated form built from Shadleaf's form controls. A failed submit renders the form again with the
 * errors; a valid one redirects back to an empty form (post/redirect/get), with a toast saying it was sent. Besides Bean Validation's field errors,
 * the controller rejects a message with a link in it as a whole, a global error that the form shows at the top, and a
 * callback date on a weekend or in the past, which the calendar does not offer.
 * {@link HtmxFormController} serves the same form, posted with htmx.
 */
@Controller
public class FormController {

  static final List<Topic> TOPICS = List.of(new Topic("sales", "handshake"), new Topic("support", "life-buoy"),
      new Topic("billing", "receipt"));
  static final List<String> REPLY_CHANNELS = List.of("email", "phone");
  private static final Pattern LINK = Pattern.compile("(?i)\\b(https?://|www\\.)");

  @ModelAttribute("topics")
  List<Topic> topics() {
    return TOPICS;
  }

  @ModelAttribute("replyChannels")
  List<String> replyChannels() {
    return REPLY_CHANNELS;
  }

  /** The first day the callback date's calendar offers. */
  @ModelAttribute("today")
  LocalDate today() {
    return LocalDate.now();
  }

  @GetMapping("/form")
  public String form(Model model) {
    model.addAttribute("contactForm", new ContactForm());
    return "form";
  }

  @PostMapping("/form")
  public String submit(@Valid @ModelAttribute("contactForm") ContactForm contactForm, BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    rejectLinks(contactForm, bindingResult);
    rejectCallbackDate(contactForm, bindingResult, today());
    if (bindingResult.hasErrors()) {
      return "form";
    }
    // The layout's toaster renders the toasts flash attribute on the page the redirect loads.
    redirectAttributes.addFlashAttribute("toasts", List.of(Toast.success("Message sent")
        .withDescription("We will reply to %s.".formatted(contactForm.getEmail()))));
    return "redirect:/form";
  }

  /** A message with a link in it is rejected as a whole: a global error, not one of the message field. */
  static void rejectLinks(ContactForm contactForm, BindingResult bindingResult) {
    if (contactForm.getMessage() != null && LINK.matcher(contactForm.getMessage()).find()) {
      bindingResult.reject("contactForm.links", "We do not accept messages with links, to keep spam out.");
    }
  }

  /**
   * A callback date must be a weekday from today on. The calendar only offers those, but a request can hold any date
   * (and without JavaScript the date input has only its min).
   */
  static void rejectCallbackDate(ContactForm contactForm, BindingResult bindingResult, LocalDate today) {
    LocalDate date = contactForm.getCallbackDate();
    if (date != null && (date.isBefore(today) || date.getDayOfWeek() == DayOfWeek.SATURDAY
        || date.getDayOfWeek() == DayOfWeek.SUNDAY)) {
      bindingResult.rejectValue("callbackDate", "contactForm.callbackDate", "must be a weekday from today on");
    }
  }

  /** A choice for the topic select: its value and the icon shown with it. */
  record Topic(String value, String icon) {
  }
}
