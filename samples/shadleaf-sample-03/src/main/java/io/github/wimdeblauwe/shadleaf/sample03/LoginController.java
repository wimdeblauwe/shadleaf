package io.github.wimdeblauwe.shadleaf.sample03;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The page to choose a provider on, one link per login client (Keycloak, and GitHub with the github profile). */
@Controller
public class LoginController {

  private final List<Provider> providers;

  public LoginController(InMemoryClientRegistrationRepository clients) {
    this.providers = StreamSupport.stream(clients.spliterator(), false)
        .filter(client -> AuthorizationGrantType.AUTHORIZATION_CODE.equals(client.getAuthorizationGrantType()))
        .map(Provider::of)
        .sorted(Comparator.comparing(Provider::name))
        .toList();
  }

  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("providers", providers);
    return "login";
  }

  public record Provider(String name, String url) {

    static Provider of(ClientRegistration client) {
      return new Provider(client.getClientName(), "/oauth2/authorization/" + client.getRegistrationId());
    }
  }
}
