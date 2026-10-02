package io.github.wimdeblauwe.shadleaf.sample02;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The Admin page: only for users with the ADMIN role, which {@link SecurityConfiguration} enforces on its URL. */
@Controller
public class AdminController {

  @GetMapping("/admin")
  public String admin(Model model) {
    model.addAttribute("accounts", SampleUsers.ACCOUNTS);
    return "admin";
  }
}
