package io.github.wimdeblauwe.shadleaf.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:user-menu>} signed in (OpenID Connect) and anonymous, in the sidebar and in the header, rendered into
 * {@code src/test/resources/approved/user-menu.approved.html} and {@code user-menu-anonymous.approved.html}. Without
 * {@code csrf()}: its token is random. {@code UserMenuComponentTest} checks the token.
 */
class UserMenuApprovalTest {

  private static final List<String> SNIPPETS = List.of(
      """
          <sl:user-menu>
            <sl:dropdown-menu-item as="a" href="/account"><sl:icon name="badge-check"/>Account</sl:dropdown-menu-item>
            <sl:dropdown-menu-item as="a" href="/settings"><sl:icon name="settings"/>Settings</sl:dropdown-menu-item>
          </sl:user-menu>""",
      "<sl:user-menu as=\"header\"/>",
      "<sl:user-menu as=\"header\" id=\"account\" side=\"left\" align=\"start\" aria-label=\"Your account\"/>");

  @Test
  void signedIn() {
    ComponentRenderTester tester = ComponentRenderTester.builder()
        .with(oidcLogin().idToken(token -> token
            .claim("name", "Ada Lovelace")
            .claim("email", "ada@example.com")
            .claim("picture", "https://example.com/ada.jpg")))
        .build();

    HtmlApproval.verifyRenders("user-menu", tester, SNIPPETS);
  }

  @Test
  void anonymous() {
    HtmlApproval.verifyRenders("user-menu-anonymous", ComponentRenderTester.create(), SNIPPETS);
  }
}
