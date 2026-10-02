package io.github.wimdeblauwe.shadleaf.sample03;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PagesController {

  @GetMapping("/")
  public String index() {
    return "index";
  }

  /** What the provider sent: an OpenID Connect user's claims, or an OAuth2 user's attributes (GitHub). */
  @GetMapping("/claims")
  public String claims(@AuthenticationPrincipal OAuth2User user, Model model) {
    Map<String, Object> attributes = new TreeMap<>(user.getAttributes());
    model.addAttribute("attributes", attributes);
    return "claims";
  }

  /** Ada's picture claim in the Keycloak realm is this application's path, so the sample needs no image host. */
  @GetMapping("/photos/{name:[a-z]+}.jpg")
  public ResponseEntity<Resource> photo(@PathVariable String name) throws IOException {
    Resource photo = new ClassPathResource("photos/" + name + ".jpg");
    return photo.exists()
        ? ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(photo)
        : ResponseEntity.notFound().build();
  }
}
