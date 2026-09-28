package io.github.wimdeblauwe.shadleaf.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderer;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class IconComponentTest {

  private final ComponentRenderer renderer = new ComponentRenderer();

  @Test
  void rendersADecorativeLucideIconInline() {
    Element svg = render("<sl:icon name=\"trash\"/>");

    assertThat(svg.tagName()).isEqualTo("svg");
    assertThat(svg.className()).isEqualTo("sl-icon");
    assertThat(svg.attr("viewBox")).isEqualTo("0 0 24 24");
    assertThat(svg.attr("width")).isEqualTo("24");
    assertThat(svg.attr("height")).isEqualTo("24");
    assertThat(svg.attr("fill")).isEqualTo("none");
    assertThat(svg.attr("stroke")).isEqualTo("currentColor");
    assertThat(svg.attr("stroke-width")).isEqualTo("2");
    assertThat(svg.attr("stroke-linecap")).isEqualTo("round");
    assertThat(svg.attr("aria-hidden")).isEqualTo("true");
    assertThat(svg.attr("focusable")).isEqualTo("false");
    assertThat(svg.hasAttr("role")).isFalse();
    assertThat(svg.hasAttr("data-icon-size")).isFalse();
    assertThat(svg.select("path")).hasSize(5);
    assertThat(svg.selectFirst("path").attr("d")).isEqualTo("M10 11v6");
    assertThat(svg.attributes().asList()).extracting(a -> a.getKey()).doesNotContain("name");
  }

  @Test
  void labelMakesItAnImageWithAnAccessibleName() {
    Element svg = render("<sl:icon name=\"circle-alert\" label=\"Warning\"/>");

    assertThat(svg.attr("role")).isEqualTo("img");
    assertThat(svg.attr("aria-label")).isEqualTo("Warning");
    assertThat(svg.hasAttr("aria-hidden")).isFalse();
    assertThat(svg.hasAttr("focusable")).isFalse();
    assertThat(svg.hasAttr("label")).isFalse();
  }

  @Test
  void blankLabelStaysDecorative() {
    Element svg = render("<sl:icon name=\"x\" label=\" \"/>");

    assertThat(svg.attr("aria-hidden")).isEqualTo("true");
    assertThat(svg.hasAttr("aria-label")).isFalse();
  }

  @Test
  void sizeAndStrokeWidth() {
    Element svg = render("<sl:icon name=\"x\" size=\"20\" stroke-width=\"1.5\"/>");

    assertThat(svg.attr("width")).isEqualTo("20");
    assertThat(svg.attr("height")).isEqualTo("20");
    assertThat(svg.attr("data-icon-size")).isEqualTo("20");
    assertThat(svg.attr("stroke-width")).isEqualTo("1.5");
    assertThat(svg.hasAttr("size")).isFalse();
  }

  @Test
  void nameCanBeAnExpression() {
    Element svg = render("<sl:icon th:name=\"${ok ? 'check' : 'x'}\"/>", Map.of("ok", true));

    assertThat(svg.selectFirst("path").attr("d")).isEqualTo("M20 6 9 17l-5-5");
  }

  @Test
  void lucideAliasesResolveToTheRenamedIcon() {
    assertThat(render("<sl:icon name=\"trash-2\"/>").html()).isEqualTo(render("<sl:icon name=\"trash\"/>").html());
    assertThat(render("<sl:icon name=\"loader-2\"/>").html())
        .isEqualTo(render("<sl:icon name=\"loader-circle\"/>").html());
  }

  @Test
  void classMergesAndOtherAttributesPassThrough() {
    Element svg = render("<sl:icon name=\"x\" class=\"text-red\" data-test=\"close\" x-show=\"open\"/>");

    assertThat(svg.className()).isEqualTo("sl-icon text-red");
    assertThat(svg.attr("data-test")).isEqualTo("close");
    assertThat(svg.attr("x-show")).isEqualTo("open");
  }

  @Test
  void unknownNameFailsWithTheClosestNames() {
    assertThatThrownBy(() -> render("<sl:icon name=\"trsh\"/>"))
        .rootCause()
        .isInstanceOf(ShadleafComponentException.class)
        .hasMessageContaining("Unknown icon 'trsh'")
        .hasMessageContaining("'trash'");
  }

  @Test
  void missingNameFails() {
    assertThatThrownBy(() -> render("<sl:icon/>"))
        .rootCause()
        .hasMessageContaining("<sl:icon> needs a name");
  }

  @Test
  void applicationIconsComeFirstAndCanReplaceBundledOnes() {
    ComponentRenderer withAppIcons = new ComponentRenderer(IconSource.classpathDirectory("icons/"));

    Element logo = Jsoup.parseBodyFragment(withAppIcons.render("<sl:icon name=\"logo\"/>")).body().child(0);
    assertThat(logo.attr("viewBox")).isEqualTo("0 0 32 32");
    assertThat(logo.attr("fill")).isEqualTo("currentColor");
    assertThat(logo.hasAttr("stroke")).isFalse();
    assertThat(logo.attr("width")).isEqualTo("24");
    assertThat(logo.className()).isEqualTo("sl-icon");
    assertThat(logo.id()).isEmpty();
    assertThat(logo.select("circle")).hasSize(1);

    Element trash = Jsoup.parseBodyFragment(withAppIcons.render("<sl:icon name=\"trash\"/>")).body().child(0);
    assertThat(trash.attr("viewBox")).isEqualTo("0 0 16 16");
    assertThat(trash.select("rect")).hasSize(1);

    Element bundled = Jsoup.parseBodyFragment(withAppIcons.render("<sl:icon name=\"x\"/>")).body().child(0);
    assertThat(bundled.attr("viewBox")).isEqualTo("0 0 24 24");
  }

  @Test
  void classpathDirectoryIgnoresNamesThatLeaveTheDirectory() {
    IconSource source = IconSource.classpathDirectory("icons");

    assertThat(source.find("logo")).isPresent();
    assertThat(source.find("../icons/logo")).isEmpty();
    assertThat(source.find("Logo")).isEmpty();
  }

  @Test
  void iconFromSvgNeedsAViewBox() {
    assertThatThrownBy(() -> Icon.fromSvg("<svg width=\"10\"><path d=\"M0 0\"/></svg>"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("viewBox");
  }

  @Test
  void iconInsideAnotherComponentsNamedSlot() {
    Element card = render("""
        <sl:test-card>
          <sl:slot name="header"><sl:icon name="x"/> Title</sl:slot>
        </sl:test-card>""");

    assertThat(card.selectFirst(".card-header > svg.sl-icon")).isNotNull();
    assertThat(card.selectFirst(".card-header").text()).isEqualTo("Title");
  }

  private Element render(String snippet) {
    return render(snippet, Map.of());
  }

  private Element render(String snippet, Map<String, ?> variables) {
    return Jsoup.parseBodyFragment(renderer.render(snippet, variables)).body().child(0);
  }
}
