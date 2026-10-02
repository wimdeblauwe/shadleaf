package io.github.wimdeblauwe.shadleaf.security;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** {@code #slUser} with the default resolver, for every kind of sign-in spring-security-test can set up. */
class CurrentUserTest {

  private static final String USER = """
      <dl>
        <dt>signedIn</dt><dd id="signed-in" th:text="${#slUser.signedIn}"></dd>
        <th:block th:if="${#slUser.signedIn}" th:with="user=${#slUser.current}">
          <dt>name</dt><dd id="name" th:text="${user.name}"></dd>
          <dt>username</dt><dd id="username" th:text="${user.username}"></dd>
          <dt>email</dt><dd id="email" th:text="${user.email}"></dd>
          <dt>picture</dt><dd id="picture" th:text="${user.picture}"></dd>
          <dt>initials</dt><dd id="initials" th:text="${user.initials}"></dd>
        </th:block>
        <dt>login</dt><dd id="login" th:text="@{${#slUser.loginUrl}}"></dd>
        <dt>logout</dt><dd id="logout" th:text="@{${#slUser.logoutUrl}}"></dd>
      </dl>""";

  @Test
  void anonymousByDefault() {
    Rendered rendered = ComponentRenderTester.create().render(USER);

    assertThat(text(rendered, "signed-in")).isEqualTo("false");
    assertThat(rendered.select("#name")).isEmpty();
    assertThat(text(rendered, "login")).isEqualTo("/login");
    assertThat(text(rendered, "logout")).isEqualTo("/logout");
  }

  @Test
  void anonymousAuthenticationIsNotSignedIn() {
    assertThat(text(render(anonymous()), "signed-in")).isEqualTo("false");
  }

  @Test
  void anUnauthenticatedTokenIsNotSignedIn() {
    TestingAuthenticationToken token = new TestingAuthenticationToken("ada", "secret");
    token.setAuthenticated(false);

    assertThat(text(render(authentication(token)), "signed-in")).isEqualTo("false");
  }

  @Test
  void openIdConnect() {
    Rendered rendered = render(oidcLogin().idToken(token -> token
        .claim("name", "Ada Lovelace")
        .claim("preferred_username", "ada")
        .claim("email", "ada@example.com")
        .claim("picture", "https://example.com/ada.jpg")));

    assertThat(text(rendered, "signed-in")).isEqualTo("true");
    assertThat(text(rendered, "name")).isEqualTo("Ada Lovelace");
    assertThat(text(rendered, "username")).isEqualTo("ada");
    assertThat(text(rendered, "email")).isEqualTo("ada@example.com");
    assertThat(text(rendered, "picture")).isEqualTo("https://example.com/ada.jpg");
    assertThat(text(rendered, "initials")).isEqualTo("AL");
  }

  @Test
  void openIdConnectWithGivenAndFamilyNameOnly() {
    Rendered rendered = render(oidcLogin().idToken(token -> token
        .claim("given_name", "Ada")
        .claim("family_name", "Lovelace")));

    assertThat(text(rendered, "name")).isEqualTo("Ada Lovelace");
    assertThat(text(rendered, "username")).isEmpty();
  }

  @Test
  void openIdConnectWithOnlyAnEmail() {
    Rendered rendered = render(oidcLogin().idToken(token -> token.claim("email", "ada.lovelace@example.com")));

    assertThat(text(rendered, "name")).isEqualTo("ada.lovelace@example.com");
    assertThat(text(rendered, "initials")).isEqualTo("AL");
  }

  @Test
  void openIdConnectWithNothingButTheSubject() {
    Rendered rendered = render(oidcLogin().idToken(token -> token.subject("248289761001")));

    assertThat(text(rendered, "name")).isEqualTo("248289761001");
    assertThat(text(rendered, "initials")).isEqualTo("2");
  }

  @Test
  void gitHub() {
    // GitHub's user-info: the numeric id is the principal's name (Spring Boot's user-name-attribute for GitHub), the
    // name can be missing, the email often is
    Rendered rendered = render(oauth2Login().attributes(attributes -> {
      attributes.put("id", 583231);
      attributes.put("login", "octocat");
      attributes.put("name", "The Octocat");
      attributes.put("email", null);
      attributes.put("avatar_url", "https://avatars.githubusercontent.com/u/583231?v=4");
    }));

    assertThat(text(rendered, "name")).isEqualTo("The Octocat");
    assertThat(text(rendered, "username")).isEqualTo("octocat");
    assertThat(text(rendered, "email")).isEmpty();
    assertThat(text(rendered, "picture")).isEqualTo("https://avatars.githubusercontent.com/u/583231?v=4");
    assertThat(text(rendered, "initials")).isEqualTo("TO");
  }

  @Test
  void gitHubWithoutAName() {
    Rendered rendered = render(oauth2Login().attributes(attributes -> {
      attributes.put("id", 583231);
      attributes.put("login", "octocat");
    }));

    assertThat(text(rendered, "name")).isEqualTo("octocat");
    assertThat(text(rendered, "initials")).isEqualTo("O");
  }

  @Test
  void userDetails() {
    Rendered rendered = render(user("ada").roles("USER"));

    assertThat(text(rendered, "name")).isEqualTo("ada");
    assertThat(text(rendered, "username")).isEqualTo("ada");
    assertThat(text(rendered, "email")).isEmpty();
    assertThat(text(rendered, "picture")).isEmpty();
    assertThat(text(rendered, "initials")).isEqualTo("A");
  }

  @Test
  void rememberMeIsSignedIn() {
    Rendered rendered = render(authentication(new RememberMeAuthenticationToken("key", "ada",
        AuthorityUtils.createAuthorityList("ROLE_USER"))));

    assertThat(text(rendered, "signed-in")).isEqualTo("true");
    assertThat(text(rendered, "name")).isEqualTo("ada");
  }

  @Test
  void anyOtherPrincipalGoesByTheAuthenticationsName() {
    Rendered rendered = render(authentication(new TestingAuthenticationToken(new Object() {
      @Override
      public String toString() {
        return "Grace Hopper";
      }
    }, null, List.of())));

    assertThat(text(rendered, "name")).isEqualTo("Grace Hopper");
    assertThat(text(rendered, "initials")).isEqualTo("GH");
  }

  @Test
  void jwtClaims() {
    ClaimAccessor jwt = () -> Map.of("sub", "1", "name", "Ada Lovelace", "email", "ada@example.com");

    Rendered rendered = render(authentication(new TestingAuthenticationToken(jwt, null, List.of())));

    assertThat(text(rendered, "name")).isEqualTo("Ada Lovelace");
    assertThat(text(rendered, "email")).isEqualTo("ada@example.com");
  }

  @Test
  void anApplicationResolverReplacesTheDefault() {
    CurrentUserResolver resolver = authentication -> ShadleafUser.of("Countess of Lovelace", "ada", null, null);
    ComponentRenderTester tester = ComponentRenderTester.builder()
        .userSource(new SpringSecurityUserSource(resolver, "/sign-in", "/sign-out"))
        .with(user("ada"))
        .build();

    Rendered rendered = tester.render(USER);

    assertThat(text(rendered, "name")).isEqualTo("Countess of Lovelace");
    assertThat(text(rendered, "initials")).isEqualTo("CL");
    assertThat(text(rendered, "login")).isEqualTo("/sign-in");
    assertThat(text(rendered, "logout")).isEqualTo("/sign-out");
  }

  @Test
  void aResolverReturningNullMeansAnonymous() {
    ComponentRenderTester tester = ComponentRenderTester.builder()
        .userSource(new SpringSecurityUserSource(authentication -> null, "/login", "/logout"))
        .with(user("ada"))
        .build();

    assertThat(text(tester.render(USER), "signed-in")).isEqualTo("false");
  }

  @Test
  void urlsIncludeTheContextPath() {
    Rendered rendered = ComponentRenderTester.builder().contextPath("/app").build().render(USER);

    assertThat(text(rendered, "login")).isEqualTo("/app/login");
    assertThat(text(rendered, "logout")).isEqualTo("/app/logout");
  }

  @Test
  void resolvesOncePerTemplate() {
    int[] calls = {0};
    CurrentUserResolver resolver = authentication -> {
      calls[0]++;
      return ShadleafUser.of("Ada", null, null, null);
    };
    ComponentRenderTester tester = ComponentRenderTester.builder()
        .userSource(new SpringSecurityUserSource(resolver, "/login", "/logout"))
        .with(user("ada"))
        .build();

    tester.render(USER);

    assertThat(calls[0]).isEqualTo(1);
  }

  @Test
  void theSignedInUserDoesNotOutliveTheRender() {
    render(user("ada"));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(text(ComponentRenderTester.create().render(USER), "signed-in")).isEqualTo("false");
  }

  private static Rendered render(RequestPostProcessor postProcessor) {
    return ComponentRenderTester.builder().with(postProcessor).build().render(USER);
  }

  private static String text(Rendered rendered, String id) {
    return rendered.select("#" + id).text();
  }
}
