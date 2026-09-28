package io.github.wimdeblauwe.shadleaf.sample02;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String index(Principal principal, Model model) {
    model.addAttribute("username", principal.getName());
    return "index";
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }
}
