package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.Valid;
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
 * errors; a valid one redirects back to an empty form (post/redirect/get). Besides Bean Validation's field errors,
 * the controller rejects a message with a link in it as a whole, a global error that the form shows at the top.
 */
@Controller
public class FormController {

  static final List<String> TOPICS = List.of("sales", "support", "billing");
  static final List<String> REPLY_CHANNELS = List.of("email", "phone");
  private static final Pattern LINK = Pattern.compile("(?i)\\b(https?://|www\\.)");

  @ModelAttribute("topics")
  List<String> topics() {
    return TOPICS;
  }

  @ModelAttribute("replyChannels")
  List<String> replyChannels() {
    return REPLY_CHANNELS;
  }

  @GetMapping("/form")
  public String form(Model model) {
    model.addAttribute("contactForm", new ContactForm());
    return "form";
  }

  @PostMapping("/form")
  public String submit(@Valid @ModelAttribute("contactForm") ContactForm contactForm, BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (contactForm.getMessage() != null && LINK.matcher(contactForm.getMessage()).find()) {
      bindingResult.reject("contactForm.links", "We do not accept messages with links, to keep spam out.");
    }
    if (bindingResult.hasErrors()) {
      return "form";
    }
    redirectAttributes.addFlashAttribute("sentTo", contactForm.getEmail());
    return "redirect:/form";
  }
}
