package io.github.wimdeblauwe.shadleaf.avatar;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.junit.jupiter.api.Test;

/**
 * What the server renders for the broken-image fallback: both parts, the image first and with no load state. Shadleaf's
 * script (js/avatar.js) marks the image {@code data-status="loaded"} or {@code "error"} in the browser, and the
 * stylesheet shows the fallback until it is loaded; {@code docs/tests/avatar.spec.ts} covers that.
 */
class AvatarComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void rendersImageAndFallbackWithoutLoadState() {
    Rendered rendered = tester.render("""
        <sl:avatar label="Jane Doe">
          <sl:avatar-image src="/users/42/photo"/>
          <sl:avatar-fallback>JD</sl:avatar-fallback>
        </sl:avatar>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasAttribute("role", "img")
        .hasAttribute("aria-label", "Jane Doe");
    // The server cannot know whether the picture will load: the browser decides, so nothing is marked yet.
    assertThat(rendered).element(".avatar > img.avatar-image:first-child")
        .hasAttribute("src", "/users/42/photo")
        .hasAttribute("alt", "")
        .hasNoAttribute("data-status");
    assertThat(rendered).element(".avatar > .avatar-fallback").hasText("JD");
  }

  @Test
  void passesImageAttributesThrough() {
    // loading="lazy" works: until it is loaded the image is invisible, not display: none, so it still loads.
    Rendered rendered = tester.render("""
        <sl:avatar>
          <sl:avatar-image th:src="@{/users/{id}/photo(id=42)}" loading="lazy" srcset="/users/42/photo?size=2x 2x"/>
          <sl:avatar-fallback>JD</sl:avatar-fallback>
        </sl:avatar>""");

    assertThat(rendered).root().hasAttribute("aria-hidden", "true").hasNoAttribute("role");
    assertThat(rendered).element(".avatar-image")
        .hasAttribute("src", "/users/42/photo")
        .hasAttribute("loading", "lazy")
        .hasAttribute("srcset", "/users/42/photo?size=2x 2x")
        .hasAttribute("alt", "");
  }

  @Test
  void nullSourceRendersAnEmptySrc() {
    Rendered rendered = tester.render("""
        <sl:avatar>
          <sl:avatar-image th:src="${null}"/>
          <sl:avatar-fallback>JD</sl:avatar-fallback>
        </sl:avatar>""");

    // The stylesheet hides an image with an empty src; no script is needed for a user without a photo.
    assertThat(rendered).element(".avatar-image").hasAttribute("src", "").hasNoAttribute("data-status");
  }
}
