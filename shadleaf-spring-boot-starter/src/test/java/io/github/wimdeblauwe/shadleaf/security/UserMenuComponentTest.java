package io.github.wimdeblauwe.shadleaf.security;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FixedUserSource;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * {@code <sl:user-menu>}: the signed-in user's menu in the sidebar and in the header, signed in with OpenID Connect,
 * GitHub (OAuth2) and a username, and the Sign in link for an anonymous visitor.
 */
class UserMenuComponentTest {

  private static final RequestPostProcessor ADA = oidcLogin().idToken(token -> token
      .claim("name", "Ada Lovelace")
      .claim("preferred_username", "ada")
      .claim("email", "ada@example.com")
      .claim("picture", "https://example.com/ada.jpg"));

  @Test
  void sidebarSignedIn() {
    Rendered rendered = render(ADA, """
        <sl:user-menu>
          <sl:dropdown-menu-item as="a" href="/account"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
        </sl:user-menu>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("ul.sidebar-menu.user-menu > li.sidebar-menu-item > button.sidebar-menu-button")
        .hasClass("dropdown-menu-trigger")
        .hasClass("user-menu-trigger")
        .hasAttribute("id", "user-menu-trigger")
        .hasAttribute("popovertarget", "user-menu")
        .hasAttribute("aria-haspopup", "menu")
        .hasAttribute("data-size", "lg")
        .hasNoAttribute("aria-label");
    // avatar, name and email, chevrons, the tooltip for icon mode
    assertThat(rendered.select(".user-menu-trigger > *").eachAttr("class")).containsExactly(
        "avatar user-menu-avatar", "sidebar-menu-button-label", "sl-icon", "tooltip-content sidebar-menu-tooltip");
    assertThat(rendered).element(".user-menu-trigger .sidebar-menu-button-label > strong").hasText("Ada Lovelace");
    assertThat(rendered).element(".user-menu-trigger .sidebar-menu-button-label > small").hasText("ada@example.com");
    assertThat(rendered).element(".user-menu-trigger .sidebar-menu-tooltip").hasText("Ada Lovelace");
    assertThat(rendered).element(".user-menu-trigger .avatar").hasAttribute("aria-hidden", "true");
    assertThat(rendered).element(".user-menu-trigger .avatar-image")
        .hasAttribute("src", "https://example.com/ada.jpg")
        .hasAttribute("alt", "");
    assertThat(rendered).element(".user-menu-trigger .avatar-fallback").hasText("AL");

    assertThat(rendered).element(".user-menu > li > .dropdown-menu-content")
        .hasAttribute("id", "user-menu")
        .hasAttribute("role", "menu")
        .hasAttribute("aria-labelledby", "user-menu-trigger")
        .hasAttribute("data-side", "right")
        .hasAttribute("data-align", "end")
        .hasClass("user-menu-content");
  }

  @Test
  void theMenuHoldsTheUserTheItemsAndSignOut() {
    Rendered rendered = render(ADA, """
        <sl:user-menu>
          <sl:dropdown-menu-item as="a" href="/account">Account</sl:dropdown-menu-item>
          <sl:dropdown-menu-item as="a" href="/settings">Settings</sl:dropdown-menu-item>
        </sl:user-menu>""");

    assertThat(rendered.select(".user-menu-content > *").eachAttr("class")).containsExactly(
        "dropdown-menu-label user-menu-label", "dropdown-menu-separator", "dropdown-menu-item",
        "dropdown-menu-item", "dropdown-menu-separator", "dropdown-menu-item user-menu-sign-out");
    assertThat(rendered).element(".user-menu-label .user-menu-name").hasText("Ada Lovelace");
    assertThat(rendered).element(".user-menu-label .user-menu-detail").hasText("ada@example.com");
    assertThat(rendered).element(".user-menu-label .avatar").hasAttribute("aria-hidden", "true");
    assertThat(rendered).element(".user-menu-sign-out")
        .hasAttribute("type", "submit")
        .hasAttribute("form", "user-menu-sign-out")
        .hasAttribute("role", "menuitem")
        .hasText("Sign out");
  }

  @Test
  void withoutItemsThereIsOneSeparator() {
    Rendered rendered = render(ADA, "<sl:user-menu/>");

    assertThat(rendered.select(".user-menu-content > *").eachAttr("class")).containsExactly(
        "dropdown-menu-label user-menu-label", "dropdown-menu-separator", "dropdown-menu-item user-menu-sign-out");
  }

  @Test
  void theSignOutFormIsOutsideTheMenuWithTheCsrfToken() {
    Rendered rendered = ComponentRenderTester.builder().with(ADA).with(csrf()).contextPath("/app").build()
        .render("<sl:user-menu/>");

    Element form = rendered.select("form#user-menu-sign-out").first();
    assertThat(form).isNotNull();
    assertThat(form.parent()).isSameAs(rendered.select(".user-menu > li").first());
    assertThat(form)
        .hasAttribute("method", "post")
        .hasAttribute("action", "/app/logout")
        .hasAttribute("hx-boost", "false")
        .hasAttribute("hidden");
    assertThat(rendered.select("form#user-menu-sign-out > input[type=hidden][name=_csrf]")).hasSize(1);
    assertThat(rendered.select("[role=menu] form")).isEmpty();
  }

  @Test
  void theSecondLineIsTheUsernameWithoutAnEmail() {
    Rendered rendered = render(oauth2Login().attributes(attributes -> {
      attributes.put("login", "octocat");
      attributes.put("name", "The Octocat");
      attributes.put("avatar_url", "https://example.com/octocat.png");
    }), "<sl:user-menu/>");

    assertThat(rendered).element(".user-menu-trigger .sidebar-menu-button-label > small").hasText("octocat");
    assertThat(rendered).element(".user-menu-label .user-menu-detail").hasText("octocat");
    assertThat(rendered).element(".user-menu-trigger .avatar-image")
        .hasAttribute("src", "https://example.com/octocat.png");
  }

  @Test
  void withoutEmailOrUsernameThereIsNoSecondLine() {
    Rendered rendered = ComponentRenderTester.builder()
        .userSource(FixedUserSource.of(new ShadleafUser("Ada", null, null, null, null)))
        .build()
        .render("<sl:user-menu/>");

    assertThat(rendered.select(".user-menu-trigger .sidebar-menu-button-label > *")).hasSize(1);
    assertThat(rendered.select(".user-menu-detail")).isEmpty();
    // no picture: the image has an empty src (CSS hides it); no initials: an icon
    assertThat(rendered).element(".user-menu-trigger .avatar-image").hasAttribute("src", "");
    assertThat(rendered.select(".user-menu-trigger .avatar-fallback > svg")).hasSize(1);
  }

  @Test
  void aUsernameAndPassword() {
    Rendered rendered = render(user("grace"), "<sl:user-menu as=\"header\"/>");

    assertThat(rendered).element(".user-menu-trigger").hasAttribute("aria-label", "Account: grace");
    assertThat(rendered).element(".user-menu-label .user-menu-name").hasText("grace");
    // The username is the name: no second line repeating it.
    assertThat(rendered.select(".user-menu-detail")).isEmpty();
    assertThat(rendered).element(".user-menu-trigger .avatar-fallback").hasText("G");
  }

  @Test
  void headerSignedIn() {
    Rendered rendered = render(ADA, """
        <sl:user-menu as="header">
          <sl:dropdown-menu-item as="a" href="/account">Account</sl:dropdown-menu-item>
        </sl:user-menu>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered.select(".sidebar-menu")).isEmpty();
    assertThat(rendered).element("button.user-menu-trigger")
        .hasClass("btn")
        .hasClass("dropdown-menu-trigger")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon")
        .hasAttribute("id", "user-menu-trigger")
        .hasAttribute("popovertarget", "user-menu")
        .hasAttribute("aria-label", "Account: Ada Lovelace");
    assertThat(rendered.select(".user-menu-trigger > *").eachAttr("class")).containsExactly("avatar user-menu-avatar");
    assertThat(rendered).element(".user-menu-trigger .avatar").hasAttribute("aria-hidden", "true");
    assertThat(rendered).element(".dropdown-menu-content")
        .hasNoAttribute("data-side")
        .hasAttribute("data-align", "end");
    assertThat(rendered.select("form#user-menu-sign-out")).hasSize(1);
  }

  @Test
  void aCallersAriaLabelAndAttributesGoToTheButton() {
    Rendered rendered = render(ADA, """
        <sl:user-menu as="header" id="account" aria-label="Your account" data-test="x" side="left" align="start"/>""");

    assertThat(rendered).element("button.user-menu-trigger")
        .hasAttribute("id", "account-trigger")
        .hasAttribute("aria-label", "Your account")
        .hasAttribute("data-test", "x");
    assertThat(rendered).element(".dropdown-menu-content")
        .hasAttribute("id", "account")
        .hasAttribute("data-side", "left")
        .hasNoAttribute("data-align");
    assertThat(rendered).element(".user-menu-sign-out").hasAttribute("form", "account-sign-out");
    assertThat(rendered.select("form#account-sign-out")).hasSize(1);
  }

  @Test
  void anonymousSidebarIsASignInLink() {
    Rendered rendered = ComponentRenderTester.builder().contextPath("/app").build().render("<sl:user-menu/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered.select(".dropdown-menu-content, form")).isEmpty();
    assertThat(rendered).element("ul.sidebar-menu.user-menu > li > a.sidebar-menu-button")
        .hasClass("user-menu-sign-in")
        .hasAttribute("href", "/app/login")
        .hasAttribute("hx-boost", "false");
    assertThat(rendered).element(".user-menu-sign-in .sidebar-menu-button-label").hasText("Sign in");
    assertThat(rendered).element(".user-menu-sign-in .sidebar-menu-tooltip").hasText("Sign in");
  }

  @Test
  void anonymousHeaderIsASignInLink() {
    Rendered rendered = ComponentRenderTester.builder()
        .userSource(UserSource.anonymous("/oauth2/authorization/github", "/logout"))
        .build()
        .render("<sl:user-menu as=\"header\" data-test=\"x\"/>");

    assertThat(rendered).element("a.btn.user-menu-sign-in")
        .hasAttribute("href", "/oauth2/authorization/github")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("hx-boost", "false")
        .hasAttribute("data-test", "x")
        .hasText("Sign in");
  }

  @Test
  void anAriaLabelOnlyNamesTheHeaderButton() {
    String snippet = "<sl:user-menu aria-label=\"Your account\"/>";

    assertThat(render(ADA, snippet)).element(".user-menu-trigger").hasNoAttribute("aria-label");
    assertThat(ComponentRenderTester.create().render(snippet)).element(".user-menu-sign-in")
        .hasNoAttribute("aria-label");
    assertThat(ComponentRenderTester.create().render("<sl:user-menu as=\"header\" aria-label=\"Your account\"/>"))
        .element(".user-menu-sign-in").hasNoAttribute("aria-label");
  }

  private static Rendered render(RequestPostProcessor login, String snippet) {
    return ComponentRenderTester.builder().with(login).build().render(snippet);
  }
}
