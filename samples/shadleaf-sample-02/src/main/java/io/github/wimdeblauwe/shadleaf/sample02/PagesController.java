package io.github.wimdeblauwe.shadleaf.sample02;

import io.github.wimdeblauwe.shadleaf.toast.Toast;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The pages of the header layout: Forms, Overlays and Data in the header's row, Settings and Help only in the phone
 * panel.
 */
@Controller
public class PagesController {

  static final List<String> PLANS = List.of("free", "team", "enterprise");

  @GetMapping("/forms")
  public String forms(Model model) {
    model.addAttribute("profileForm", new ProfileForm());
    model.addAttribute("plans", PLANS);
    return "forms";
  }

  @PostMapping("/forms")
  public String saveForms(@ModelAttribute ProfileForm profileForm, BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
    if (profileForm.getName() == null || profileForm.getName().isBlank()) {
      result.rejectValue("name", "required", "Enter your name.");
    }
    if (result.hasErrors()) {
      model.addAttribute("plans", PLANS);
      return "forms";
    }
    redirectAttributes.addFlashAttribute("toasts", List.of(Toast.success("Profile saved")));
    return "redirect:/forms";
  }

  @GetMapping("/overlays")
  public String overlays() {
    return "overlays";
  }

  @GetMapping("/data")
  public String data(@RequestParam(defaultValue = "projects") String tab, Model model) {
    model.addAttribute("tab", "about".equals(tab) ? "about" : "projects");
    model.addAttribute("projects", List.of(
        new Project("Marketing site", true, 4),
        new Project("Android app", true, 6),
        new Project("Brand guidelines", false, 2)));
    return "data";
  }

  @GetMapping("/settings")
  public String settings() {
    return "settings";
  }

  @GetMapping("/help")
  public String help() {
    return "help";
  }

  /** A bean, not a record: an unchecked checkbox sends no value, which a record's boolean could not take. */
  public static class ProfileForm {

    private String name = "";
    private String email = "";
    private String plan = "team";
    private boolean newsletter;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPlan() {
      return plan;
    }

    public void setPlan(String plan) {
      this.plan = plan;
    }

    public boolean isNewsletter() {
      return newsletter;
    }

    public void setNewsletter(boolean newsletter) {
      this.newsletter = newsletter;
    }
  }

  public record Project(String name, boolean active, int members) {

  }
}
