package io.github.wimdeblauwe.shadleaf.avatar;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:avatar>} and its parts, named and decorative, with and without an image source (also a lazy one),
 * rendered into {@code src/test/resources/approved/avatar.approved.html}.
 */
class AvatarApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void sizesNamesAndSources() {
    HtmlApproval.verifyRenders("avatar", tester, List.of(
        "<sl:avatar><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>",
        "<sl:avatar size=\"sm\"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>",
        "<sl:avatar size=\"default\"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>",
        "<sl:avatar size=\"lg\"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>",
        "<sl:avatar label=\"Jane Doe\"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>",
        "<sl:avatar aria-labelledby=\"author-name\"><sl:avatar-fallback>JD</sl:avatar-fallback></sl:avatar>",
        """
            <sl:avatar label="Jane Doe">
              <sl:avatar-image th:src="@{/users/42/photo}"/>
              <sl:avatar-fallback>JD</sl:avatar-fallback>
            </sl:avatar>""",
        """
            <sl:avatar>
              <sl:avatar-image th:src="${null}"/>
              <sl:avatar-fallback>JD</sl:avatar-fallback>
            </sl:avatar>""",
        // The server never marks the load state (data-status): Shadleaf's script does, in the browser.
        """
            <sl:avatar>
              <sl:avatar-image src="/users/7/photo" loading="lazy"/>
              <sl:avatar-fallback>GH</sl:avatar-fallback>
            </sl:avatar>""",
        "<sl:avatar><sl:avatar-fallback><sl:icon name=\"user\"/></sl:avatar-fallback></sl:avatar>",
        "<sl:avatar-image src=\"/users/42/photo\" alt=\"Jane Doe\" class=\"grayscale\"/>"));
  }
}
