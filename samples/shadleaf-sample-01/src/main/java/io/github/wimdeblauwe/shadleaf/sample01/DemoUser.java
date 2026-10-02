package io.github.wimdeblauwe.shadleaf.sample01;

import io.github.wimdeblauwe.shadleaf.security.ShadleafUser;
import io.github.wimdeblauwe.shadleaf.security.UserSource;
import io.github.wimdeblauwe.shadleaf.toast.Toast;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The user the sidebar's user menu shows. This sample has no Spring Security, so nobody would ever be signed in: a
 * {@link UserSource} bean of the application's own replaces the starter's and says Ada Lovelace always is (with her
 * photo from the members page). Signing out for real is sample-02's: here Sign out posts to {@code /sign-out}, which
 * only says so.
 */
@Configuration(proxyBeanMethods = false)
public class DemoUser {

  static final ShadleafUser ADA = ShadleafUser.of("Ada Lovelace", "ada", "ada@example.com", "/dialog/members/1/photo");

  @Bean
  UserSource demoUserSource() {
    return new UserSource() {
      @Override
      public ShadleafUser currentUser() {
        return ADA;
      }

      @Override
      public String loginUrl() {
        return "/";
      }

      @Override
      public String logoutUrl() {
        return "/sign-out";
      }
    };
  }

  @Controller
  static class SignOutController {

    @PostMapping("/sign-out")
    public String signOut(RedirectAttributes redirectAttributes) {
      redirectAttributes.addFlashAttribute("toasts", List.of(Toast.info("Still signed in")
          .withDescription("This sample has no Spring Security. Sample-02 signs out for real.")));
      return "redirect:/";
    }
  }
}
