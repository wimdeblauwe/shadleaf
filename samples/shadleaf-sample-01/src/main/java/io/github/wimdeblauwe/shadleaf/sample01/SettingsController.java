package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The settings page: tabs whose triggers are links to {@code /settings?tab=...}, so the page works without
 * JavaScript, and switch in place with it. The sessions tab loads its panel with htmx the first time it is shown,
 * unless the page was requested with that tab active, when the server renders it. The help tab holds an FAQ accordion,
 * with the question from {@code ?question=} open; the notifications tab a collapsible.
 */
@Controller
public class SettingsController {

  static final List<String> TABS = List.of("account", "notifications", "sessions", "help");

  private static final List<Question> FAQ = List.of(
      new Question("password", "How do I change my password?",
          "Choose \"Forgot password\" on the sign-in page and follow the link we email you."),
      new Question("export", "Can I export my data?",
          "Yes. Settings, then Account, then Export: you get a zip of everything you added, within an hour."),
      new Question("delete", "How do I delete my account?",
          "Ask your team's administrator. Deleted accounts are kept for 30 days in case you change your mind."));

  @GetMapping("/settings")
  public String settings(@RequestParam(defaultValue = "account") String tab,
      @RequestParam(required = false) String question, Model model) {
    model.addAttribute("tab", TABS.contains(tab) ? tab : "account");
    model.addAttribute("faq", FAQ);
    model.addAttribute("openQuestion", question);
    model.addAttribute("sessions", sessions());
    return "settings";
  }

  @HxRequest
  @GetMapping("/settings/sessions")
  public String sessionsPanel(Model model) {
    model.addAttribute("sessions", sessions());
    return "settings :: sessions";
  }

  private static List<Session> sessions() {
    return List.of(new Session("Firefox on Linux", "Antwerp", "Now"),
        new Session("Safari on iPhone", "Ghent", "2 hours ago"));
  }

  public record Question(String id, String title, String answer) {
  }

  public record Session(String device, String place, String lastSeen) {
  }
}
