package io.github.wimdeblauwe.shadleaf.sample03;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * With OAuth2 login, an htmx request after the session expired is sent to its page (HX-Redirect), which then goes to
 * Keycloak as a normal navigation: an htmx request cannot follow a redirect to another origin.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionExpiryTest {

  @DynamicPropertySource
  static void keycloak(DynamicPropertyRegistry registry) {
    KeycloakContainer.register(registry);
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  void aBoostedLinkIsSentToThePageItLinksTo() throws Exception {
    mockMvc.perform(get("/claims")
            .header("HX-Request", "true")
            .header("HX-Boosted", "true")
            .header("HX-Current-URL", "http://localhost/"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("HX-Redirect", "/claims"))
        .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
  }

  @Test
  void aBoostedFormWhoseSessionExpiredIsSentToItsPage() throws Exception {
    mockMvc.perform(post("/logout")
            .param("_csrf", "token-of-the-expired-session")
            .header("HX-Request", "true")
            .header("HX-Boosted", "true")
            .header("HX-Current-URL", "http://localhost/claims"))
        .andExpect(status().isForbidden())
        .andExpect(header().string("HX-Redirect", "/claims"));
  }

  @Test
  void aNormalRequestGoesStraightToKeycloak() throws Exception {
    mockMvc.perform(get("/claims"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
  }
}
