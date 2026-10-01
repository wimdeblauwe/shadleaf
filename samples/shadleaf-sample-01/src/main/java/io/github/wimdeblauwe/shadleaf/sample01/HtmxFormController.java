package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
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
 * The form of {@link FormController}, posted with htmx. htmx sends {@code HX-Request}, and the answer is then the
 * {@code contact} fragment alone, with the errors or the confirmation, which htmx swaps in place of the old one. There
 * is no redirect in that case: the POST was not a navigation, so a reload cannot send it again. Without JavaScript the
 * form posts normally, and gets the page back, or a redirect to the confirmation (post/redirect/get).
 */
@Controller
public class HtmxFormController {

  @ModelAttribute("topics")
  List<FormController.Topic> topics() {
    return FormController.TOPICS;
  }

  @ModelAttribute("replyChannels")
  List<String> replyChannels() {
    return FormController.REPLY_CHANNELS;
  }

  @GetMapping("/htmx-form")
  public String form(Model model) {
    model.addAttribute("contactForm", new ContactForm());
    return "htmx-form";
  }

  @PostMapping("/htmx-form")
  public String submit(@Valid @ModelAttribute("contactForm") ContactForm contactForm, BindingResult bindingResult,
      HtmxRequest htmxRequest, Model model, RedirectAttributes redirectAttributes) {
    FormController.rejectLinks(contactForm, bindingResult);
    if (HtmxRequests.wantsFragment(htmxRequest)) {
      if (!bindingResult.hasErrors()) {
        model.addAttribute("sentTo", contactForm.getEmail());
      }
      return "htmx-form :: contact";
    }
    if (bindingResult.hasErrors()) {
      return "htmx-form";
    }
    redirectAttributes.addFlashAttribute("sentTo", contactForm.getEmail());
    return "redirect:/htmx-form";
  }
}
