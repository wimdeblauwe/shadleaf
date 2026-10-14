package io.github.wimdeblauwe.shadleaf.dialog;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@code <sl:dialog-body>} and {@code <sl:sheet-body>}: the same markup, each with its own class names. */
class DialogBodyComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void anEmptyBodyIsAnHtmxTargetThatIsEmptiedOnClose(String kind) {
    Rendered rendered = tester.render("<sl:%1$s id=\"edit\"><sl:%1$s-body/></sl:%1$s>".formatted(kind));

    assertThat(rendered).hasNoLeakedMarkup();
    Element body = rendered.root().selectFirst(":root > ." + kind + "-body");
    assertThat(body).hasNoAttribute("data-on-load-error").hasNoAttribute("data-loading");
    assertThat(body.children()).extracting(Element::className)
        .containsExactly("alert " + kind + "-body-error", kind + "-body-content", kind + "-body-loading");

    Element content = body.selectFirst("." + kind + "-body-content");
    assertThat(content)
        .hasAttribute("id", "edit-body")
        .hasAttribute("data-clear-on-close", "true")
        .hasNoAttribute("aria-busy");
    assertThat(content.childNodeSize()).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void contentFromThePageStays(String kind) {
    Element content = tester.render("<sl:%1$s id=\"help\"><sl:%1$s-body><p>Some help</p></sl:%1$s-body></sl:%1$s>"
        .formatted(kind)).root().selectFirst("." + kind + "-body-content");

    assertThat(content).hasAttribute("id", "help-body").hasNoAttribute("data-clear-on-close");
    assertThat(content.selectFirst("p")).hasText("Some help");
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void theErrorIsAHiddenDestructiveAlert(String kind) {
    Element error = tester.render("<sl:%1$s id=\"edit\"><sl:%1$s-body/></sl:%1$s>".formatted(kind)).root()
        .selectFirst("." + kind + "-body-error");

    assertThat(error)
        .hasClass("alert")
        .hasAttribute("data-variant", "destructive")
        .hasAttribute("role", "alert")
        .hasAttribute("hidden");
    assertThat(error.selectFirst(":root > svg")).isNotNull();
    assertThat(error.selectFirst(".alert-title")).hasText("Something went wrong. Please try again.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void theErrorSlotReplacesTheMessage(String kind) {
    Element error = tester.render("""
        <sl:%1$s id="edit"><sl:%1$s-body>
          <sl:slot name="error">The member could not be loaded.</sl:slot>
        </sl:%1$s-body></sl:%1$s>""".formatted(kind)).root().selectFirst("." + kind + "-body-error");

    assertThat(error.selectFirst(".alert-title")).hasText("The member could not be loaded.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void onlyANamedSlotLeavesTheBodyEmpty(String kind) {
    Element content = tester.render("""
        <sl:%1$s id="edit"><sl:%1$s-body>
          <sl:slot name="error">The member could not be loaded.</sl:slot>
        </sl:%1$s-body></sl:%1$s>""".formatted(kind)).root().selectFirst("." + kind + "-body-content");

    assertThat(content).hasAttribute("data-clear-on-close", "true");
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void loadErrorInline(String kind) {
    Element body = tester.render("<sl:%1$s id=\"edit\"><sl:%1$s-body load-error=\"inline\"/></sl:%1$s>"
        .formatted(kind)).root().selectFirst("." + kind + "-body");

    assertThat(body).hasAttribute("data-on-load-error", "inline");
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void theLoadingLayerIsHiddenFromAssistiveTechnology(String kind) {
    Element loading = tester.render("<sl:%1$s id=\"edit\"><sl:%1$s-body/></sl:%1$s>".formatted(kind)).root()
        .selectFirst("." + kind + "-body-loading");

    assertThat(loading).hasAttribute("aria-hidden", "true");
    assertThat(loading.selectFirst("svg." + kind + "-body-spinner")).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void otherAttributesGoToTheBody(String kind) {
    Element body = tester.render("<sl:%1$s id=\"edit\"><sl:%1$s-body class=\"wide\" data-test=\"x\"/></sl:%1$s>"
        .formatted(kind)).root().selectFirst("." + kind + "-body");

    assertThat(body).hasClass("wide").hasAttribute("data-test", "x");
  }

  @ParameterizedTest
  @ValueSource(strings = {"dialog", "sheet"})
  void aPerRowIdGivesThePerRowBodyId(String kind) {
    Element content = tester.render("<sl:%1$s th:id=\"|edit-${42}|\"><sl:%1$s-body/></sl:%1$s>".formatted(kind))
        .root().selectFirst("." + kind + "-body-content");

    assertThat(content).hasAttribute("id", "edit-42-body");
  }
}
