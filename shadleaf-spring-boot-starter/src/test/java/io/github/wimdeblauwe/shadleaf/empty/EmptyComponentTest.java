package io.github.wimdeblauwe.shadleaf.empty;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class EmptyComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void hasNoBorderByDefault() {
    Rendered rendered = tester.render("<sl:empty>Content</sl:empty>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClassName("empty").hasNoAttribute("data-border");
  }

  @Test
  void borderDrawsADashedLine() {
    assertThat(tester.render("<sl:empty border>Content</sl:empty>")).root().hasAttribute("data-border", "dashed");
  }

  @Test
  void titleIsAHeadingOnlyWithALevel() {
    assertThat(tester.render("<sl:empty-title>None</sl:empty-title>")).root()
        .hasNoAttribute("role")
        .hasNoAttribute("aria-level");
    assertThat(tester.render("<sl:empty-title level=\"2\">None</sl:empty-title>")).root()
        .hasAttribute("role", "heading")
        .hasAttribute("aria-level", "2");
  }

  @Test
  void iconMedia() {
    assertThat(tester.render("<sl:empty-media>x</sl:empty-media>")).root().hasNoAttribute("data-variant");
    assertThat(tester.render("<sl:empty-media variant=\"icon\">x</sl:empty-media>")).root()
        .hasAttribute("data-variant", "icon");
  }

  @Test
  void fillsTheRowOfAnEmptyTable() {
    Element row = tester.render("""
        <table><tbody>
          <sl:table-empty colspan="3">
            <sl:empty>
              <sl:empty-header><sl:empty-title>No people yet</sl:empty-title></sl:empty-header>
            </sl:empty>
          </sl:table-empty>
        </tbody></table>""").root().selectFirst("tr");

    assertThat(row.selectFirst("td[colspan=3] > .empty > .empty-header > .empty-title")).hasText("No people yet");
  }
}
