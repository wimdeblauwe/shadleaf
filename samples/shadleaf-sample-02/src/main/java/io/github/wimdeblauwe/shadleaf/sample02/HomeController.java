package io.github.wimdeblauwe.shadleaf.sample02;

import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  /** A month of the dashboard's chart: sign-ins on the web and in the app. */
  public record MonthlySignIns(YearMonth month, int web, int app) {

  }

  private static final List<MonthlySignIns> SIGN_INS = List.of(
      new MonthlySignIns(YearMonth.of(2026, 5), 186, 80),
      new MonthlySignIns(YearMonth.of(2026, 6), 305, 200),
      new MonthlySignIns(YearMonth.of(2026, 7), 237, 120),
      new MonthlySignIns(YearMonth.of(2026, 8), 73, 190),
      new MonthlySignIns(YearMonth.of(2026, 9), 209, 130),
      new MonthlySignIns(YearMonth.of(2026, 10), 214, 140));

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("signIns", SIGN_INS);
    return "index";
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }
}
