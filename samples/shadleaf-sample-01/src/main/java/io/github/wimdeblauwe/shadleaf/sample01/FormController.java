package io.github.wimdeblauwe.shadleaf.sample01;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A server-side validated form built from Shadleaf's form controls. A failed submit renders the form again with the
 * errors; a valid one redirects back to an empty form (post/redirect/get).
 */
@Controller
public class FormController {

  static final List<String> TOPICS = List.of("sales", "support", "billing");

  @ModelAttribute("topics")
  List<String> topics() {
    return TOPICS;
  }

  @GetMapping("/form")
  public String form(Model model) {
    model.addAttribute("contactForm", new ContactForm());
    return "form";
  }

  @PostMapping("/form")
  public String submit(@Valid @ModelAttribute("contactForm") ContactForm contactForm, BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      return "form";
    }
    redirectAttributes.addFlashAttribute("sentTo", contactForm.getEmail());
    return "redirect:/form";
  }
}
