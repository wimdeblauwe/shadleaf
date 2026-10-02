package io.github.wimdeblauwe.shadleaf.sample03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The github profile: GitHub next to Keycloak. Two clients, so Sign in goes to Spring Security's page to choose one;
 * a GitHub user has a login and often no public email, so the login is the user menu's second line.
 */
@SpringBootTest(properties = {"GITHUB_CLIENT_ID=test-client", "GITHUB_CLIENT_SECRET=test-secret"})
@AutoConfigureMockMvc
@ActiveProfiles("github")
class GitHubSignInTest {

  @DynamicPropertySource
  static void keycloak(DynamicPropertyRegistry registry) {
    KeycloakContainer.register(registry);
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ClientRegistrationRepository clients;

  @Test
  void anAnonymousVisitorChoosesAProvider() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/login"));

    Document login = Jsoup.parse(mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    assertThat(login.select(".page h1").text()).isEqualTo("Sign in");
    assertThat(login.select(".sign-in-providers a").eachAttr("href"))
        .containsExactly("/oauth2/authorization/github", "/oauth2/authorization/keycloak");
    assertThat(login.select(".sign-in-providers a").eachText())
        .containsExactly("Sign in with GitHub", "Sign in with Keycloak");
    assertThat(login.select(".sign-in-providers a").eachAttr("hx-boost")).containsOnly("false");
  }

  @Test
  void aFailedSignInSaysSo() throws Exception {
    Document login = Jsoup.parse(mockMvc.perform(get("/login").queryParam("error", ""))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());

    assertThat(login.select(".alert").text()).contains("Signing in did not work");
  }

  @Test
  void gitHubSendsTheUserToGitHub() throws Exception {
    String location = mockMvc.perform(get("/oauth2/authorization/github"))
        .andExpect(status().isFound())
        .andReturn().getResponse().getRedirectedUrl();

    assertThat(location).startsWith("https://github.com/login/oauth/authorize?").contains("client_id=test-client");
  }

  @Test
  void theUserMenuShowsTheLoginWhenGitHubSendsNoEmail() throws Exception {
    Document home = page("/", octocat());

    Element trigger = home.selectFirst("#user-menu-trigger");
    assertThat(trigger.select("strong").text()).isEqualTo("The Octocat");
    assertThat(trigger.select("small").text()).isEqualTo("octocat");
    assertThat(trigger.select("img.avatar-image").attr("src"))
        .isEqualTo("https://avatars.githubusercontent.com/u/583231?v=4");
    assertThat(home.select("#user td").eachText()).containsSequence("#slUser.current.email", "(none)");
  }

  @Test
  void signingOutOfGitHubOnlyLeavesTheApplication() throws Exception {
    // GitHub has no end-session endpoint: the OpenID Connect handler falls back to the home page.
    mockMvc.perform(post("/logout").with(octocat()).with(csrf()))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("/"));
  }

  private OAuth2LoginRequestPostProcessor octocat() {
    return oauth2Login()
        .clientRegistration(clients.findByRegistrationId("github"))
        .attributes(attributes -> {
          attributes.put("id", 583231);
          attributes.put("login", "octocat");
          attributes.put("name", "The Octocat");
          attributes.put("email", null);
          attributes.put("avatar_url", "https://avatars.githubusercontent.com/u/583231?v=4");
        });
  }

  private Document page(String path, OAuth2LoginRequestPostProcessor user) throws Exception {
    return Jsoup.parse(mockMvc.perform(get(path).with(user))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }
}
