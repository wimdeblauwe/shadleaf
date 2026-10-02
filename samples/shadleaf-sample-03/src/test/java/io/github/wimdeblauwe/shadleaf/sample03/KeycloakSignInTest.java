package io.github.wimdeblauwe.shadleaf.sample03;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The real thing, over HTTP: a browser-like client (cookies, redirects followed one by one) signs in at a Keycloak
 * started by Testcontainers, sees the user menu filled from the ID token, and signs out at Keycloak too.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class KeycloakSignInTest {

  @DynamicPropertySource
  static void keycloak(DynamicPropertyRegistry registry) {
    KeycloakContainer.register(registry);
  }

  @LocalServerPort
  int port;

  HttpClient browser;

  @BeforeEach
  void newBrowser() {
    browser = HttpClient.newBuilder()
        .cookieHandler(new LocalhostIsSecure(new CookieManager()).handler())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  @Test
  void anAnonymousVisitorGoesStraightToKeycloak() throws Exception {
    HttpResponse<String> home = get(app("/"));

    assertThat(home.statusCode()).isEqualTo(302);
    assertThat(location(home)).isEqualTo(app("/oauth2/authorization/keycloak"));
    HttpResponse<String> authorize = get(location(home));
    assertThat(location(authorize)).startsWith(KeycloakContainer.issuer() + "/protocol/openid-connect/auth?");
  }

  @Test
  void theUserMenuShowsTheSignedInUserFromTheIdToken() throws Exception {
    Document home = signIn("ada", "/");

    assertThat(home.select(".page h1").text()).isEqualTo("Home");
    Element trigger = home.selectFirst("#user-menu-trigger");
    assertThat(trigger).isNotNull();
    assertThat(trigger.select("strong").text()).isEqualTo("Ada Lovelace");
    assertThat(trigger.select("small").text()).isEqualTo("ada@example.com");
    // The picture claim (a user attribute in the realm) is a path of this application.
    assertThat(trigger.select("img.avatar-image").attr("src")).isEqualTo("/photos/ada.jpg");
    assertThat(home.select("#user td").eachText())
        .containsSequence("#slUser.current.name", "Ada Lovelace")
        .containsSequence("#slUser.current.username", "ada")
        .containsSequence("#slUser.current.initials", "AL");
  }

  @Test
  void withoutAPictureTheAvatarShowsTheInitials() throws Exception {
    Document home = signIn("grace", "/");

    Element trigger = home.selectFirst("#user-menu-trigger");
    assertThat(trigger.select("strong").text()).isEqualTo("Grace Hopper");
    assertThat(trigger.select("img.avatar-image").attr("src")).isEmpty();
    assertThat(trigger.select(".avatar-fallback").text()).isEqualTo("GH");
  }

  @Test
  void afterSigningInTheUserIsBackOnThePageTheyAskedFor() throws Exception {
    Document claims = signIn("ada", "/claims");

    assertThat(claims.select(".page h1").text()).isEqualTo("Claims");
    assertThat(claims.select("#claims td").eachText())
        .containsSequence("email", "ada@example.com")
        .containsSequence("preferred_username", "ada");
  }

  @Test
  void signingOutEndsTheSessionAtKeycloakToo() throws Exception {
    Document home = signIn("ada", "/");
    Element form = home.selectFirst("form#user-menu-sign-out");
    assertThat(form).isNotNull();

    HttpResponse<String> signOut = post(app(form.attr("action")),
        Map.of("_csrf", form.selectFirst("input[name=_csrf]").val()));

    // Keycloak's end-session endpoint, with the ID token as a hint and the way back.
    assertThat(signOut.statusCode()).isEqualTo(302);
    URI endSession = URI.create(location(signOut));
    assertThat(location(signOut)).startsWith(KeycloakContainer.issuer() + "/protocol/openid-connect/logout?");
    assertThat(endSession.getQuery()).contains("id_token_hint=", "post_logout_redirect_uri=" + app("/"));

    HttpResponse<String> backHome = get(location(signOut));
    assertThat(location(backHome)).isEqualTo(app("/"));
    // Home asks to sign in again, and Keycloak no longer knows the user: its login form, not a code for the app.
    HttpResponse<String> keycloak = get(location(get(location(get(app("/"))))));
    assertThat(keycloak.statusCode()).isEqualTo(200);
    assertThat(Jsoup.parse(keycloak.body()).select("form#kc-form-login")).hasSize(1);
  }

  @Test
  void signingOutNeedsTheCsrfToken() throws Exception {
    signIn("ada", "/");

    HttpResponse<String> signOut = post(app("/logout"), Map.of());

    assertThat(signOut.statusCode()).isEqualTo(403);
    assertThat(get(app("/")).statusCode()).isEqualTo(200);
  }

  /** Asks for {@code path}, signs in on Keycloak's form and follows the redirects back; returns the page. */
  private Document signIn(String username, String path) throws Exception {
    HttpResponse<String> response = get(app(path));
    while (response.statusCode() == 302) {
      response = get(location(response));
    }
    Element form = Jsoup.parse(response.body()).selectFirst("form#kc-form-login");
    assertThat(form).as("Keycloak's login form").isNotNull();
    response = post(form.attr("action"), Map.of("username", username, "password", "password"));
    while (response.statusCode() == 302) {
      response = get(location(response));
    }
    assertThat(response.statusCode()).isEqualTo(200);
    // Spring Security's saved request, marked with ?continue so it is looked up only then.
    assertThat(response.uri().toString()).isEqualTo(app(path) + "?continue");
    return Jsoup.parse(response.body(), response.uri().toString());
  }

  private String app(String path) {
    return "http://localhost:" + port + path;
  }

  private HttpResponse<String> get(String url) throws IOException, InterruptedException {
    return browser.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String url, Map<String, String> form) throws IOException, InterruptedException {
    String body = new LinkedHashMap<>(form).entrySet().stream()
        .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
            + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
    return browser.send(HttpRequest.newBuilder(URI.create(url))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build(), HttpResponse.BodyHandlers.ofString());
  }

  /**
   * Keycloak marks its cookies {@code Secure} also over http. Browsers send them to {@code http://localhost}, a secure
   * context; Java's cookie manager only over https, so Keycloak would find no session. This one sees localhost as https.
   */
  private record LocalhostIsSecure(CookieManager cookies) {

    CookieHandler handler() {
      return new CookieHandler() {
        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> headers) throws IOException {
          return cookies.get(secure(uri), headers);
        }

        @Override
        public void put(URI uri, Map<String, List<String>> headers) throws IOException {
          cookies.put(secure(uri), headers);
        }
      };
    }

    private static URI secure(URI uri) {
      return "localhost".equals(uri.getHost()) ? URI.create(uri.toString().replaceFirst("^http:", "https:")) : uri;
    }
  }

  private static String location(HttpResponse<?> response) {
    String location = response.headers().firstValue("Location").orElseThrow(
        () -> new AssertionError("no Location header in a " + response.statusCode() + " from " + response.uri()));
    return response.uri().resolve(location).toString();
  }
}
