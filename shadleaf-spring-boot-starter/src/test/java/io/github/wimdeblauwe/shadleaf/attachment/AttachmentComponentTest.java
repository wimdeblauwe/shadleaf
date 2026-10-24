package io.github.wimdeblauwe.shadleaf.attachment;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttachmentComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void rendersNoDataAttributesForTheDefaults() {
    Rendered rendered = tester.render("<sl:attachment>Content</sl:attachment>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClassName("attachment")
        .hasNoAttribute("data-state", "data-size", "data-orientation", "data-width", "aria-busy");
  }

  @Test
  void rendersStateSizeOrientationAndWidth() {
    assertThat(tester.render("""
        <sl:attachment state="idle" size="xs" orientation="vertical" full-width>Content</sl:attachment>""")).root()
        .hasAttribute("data-state", "idle")
        .hasAttribute("data-size", "xs")
        .hasAttribute("data-orientation", "vertical")
        .hasAttribute("data-width", "full");
  }

  @ParameterizedTest
  @ValueSource(strings = {"uploading", "processing"})
  void isBusyWhileItUploadsOrIsProcessed(String state) {
    assertThat(tester.render("<sl:attachment state=\"%s\">Content</sl:attachment>".formatted(state))).root()
        .hasAttribute("data-state", state)
        .hasAttribute("aria-busy", "true");
  }

  @ParameterizedTest
  @ValueSource(strings = {"idle", "error", "done"})
  void isNotBusyOtherwise(String state) {
    assertThat(tester.render("<sl:attachment state=\"%s\">Content</sl:attachment>".formatted(state))).root()
        .hasNoAttribute("aria-busy");
  }

  @Test
  void isNoLiveRegion() {
    // Server-rendered: a live region or role="alert" would be announced on every page load.
    assertThat(tester.render("<sl:attachment state=\"error\">Upload failed</sl:attachment>")).root()
        .hasNoAttribute("role", "aria-live");
  }

  @Test
  void mediaIsAnIconByDefault() {
    assertThat(tester.render("<sl:attachment-media>x</sl:attachment-media>")).root()
        .hasClassName("attachment-media")
        .hasNoAttribute("data-variant");
    assertThat(tester.render("<sl:attachment-media variant=\"image\">x</sl:attachment-media>")).root()
        .hasAttribute("data-variant", "image");
  }

  @Test
  void titleAndDescriptionAreSpans() {
    assertThat(tester.render("<sl:attachment-title>report.pdf</sl:attachment-title>")).root()
        .hasTag("span").hasClassName("attachment-title").hasText("report.pdf");
    assertThat(tester.render("<sl:attachment-description>PDF</sl:attachment-description>")).root()
        .hasTag("span").hasClassName("attachment-description").hasText("PDF");
  }

  @Test
  void actionIsAGhostExtraSmallIconButton() {
    assertThat(tester.render("""
        <sl:attachment-action aria-label="Remove report.pdf"><sl:icon name="x"/></sl:attachment-action>""")).root()
        .hasTag("button")
        .hasClass("button", "attachment-action")
        .hasAttribute("type", "button")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon-xs")
        .hasAttribute("aria-label", "Remove report.pdf");
  }

  @Test
  void actionCanBeADownloadLink() {
    assertThat(tester.render("""
        <sl:attachment-action as="a" href="/files/1" download size="icon-sm" variant="secondary"
                              aria-label="Download"><sl:icon name="download"/></sl:attachment-action>""")).root()
        .hasTag("a")
        .hasClass("button", "attachment-action")
        .hasAttribute("href", "/files/1")
        .hasAttribute("download")
        .hasAttribute("data-variant", "secondary")
        .hasAttribute("data-size", "icon-sm");
  }

  @Test
  void iconActionNeedsAnAccessibleName() {
    assertThatRenderFailure(() -> tester.render("<sl:attachment-action><sl:icon name=\"x\"/></sl:attachment-action>"))
        .hasMessageContaining("<sl:attachment-action")
        .hasMessageContaining("aria-label");
  }

  @Test
  void actionWithTextNeedsNoAccessibleName() {
    assertThat(tester.render("<sl:attachment-action size=\"xs\">Retry</sl:attachment-action>")).root()
        .hasTag("button")
        .hasText("Retry");
  }

  @Test
  void triggerIsAButtonThatOpensADialog() {
    assertThat(tester.render("""
        <sl:attachment-trigger aria-label="Preview report.pdf" commandfor="preview" command="show-modal"/>""")).root()
        .hasTag("button")
        .hasClassName("attachment-trigger")
        .hasAttribute("type", "button")
        .hasAttribute("commandfor", "preview")
        .hasAttribute("command", "show-modal")
        .hasAttribute("aria-label", "Preview report.pdf");
  }

  @Test
  void triggerCanBeALink() {
    assertThat(tester.render("""
        <sl:attachment-trigger as="a" href="/files/1" aria-label="Open report.pdf"/>""")).root()
        .hasTag("a")
        .hasClassName("attachment-trigger")
        .hasAttribute("href", "/files/1")
        .hasNoAttribute("type");
  }

  @Test
  void triggerAlwaysNeedsAnAccessibleName() {
    assertThatRenderFailure(() -> tester.render("<sl:attachment-trigger as=\"a\" href=\"/files/1\"/>"))
        .hasMessageContaining("<sl:attachment-trigger")
        .hasMessageContaining("aria-label");
  }

  @Test
  void groupIsAPlainRowWithoutALabel() {
    assertThat(tester.render("<sl:attachment-group>x</sl:attachment-group>")).root()
        .hasClassName("attachment-group")
        .hasNoAttribute("role", "tabindex", "aria-label");
  }

  @Test
  void groupWithALabelIsAFocusableNamedGroup() {
    assertThat(tester.render("<sl:attachment-group label=\"Photos\">x</sl:attachment-group>")).root()
        .hasAttribute("role", "group")
        .hasAttribute("tabindex", "0")
        .hasAttribute("aria-label", "Photos");
  }

  @Test
  void composesIntoACard() {
    Rendered rendered = tester.render("""
        <sl:attachment>
          <sl:attachment-media><sl:icon name="file-text"/></sl:attachment-media>
          <sl:attachment-content>
            <sl:attachment-title>report.pdf</sl:attachment-title>
            <sl:attachment-description>PDF · 2.4 MB</sl:attachment-description>
          </sl:attachment-content>
          <sl:attachment-actions>
            <sl:attachment-action aria-label="Remove report.pdf"><sl:icon name="x"/></sl:attachment-action>
          </sl:attachment-actions>
          <sl:attachment-trigger as="a" href="/files/1" aria-label="Open report.pdf"/>
        </sl:attachment>""");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element(".attachment > .attachment-media > svg").hasAttribute("aria-hidden", "true");
    assertThat(rendered).element(".attachment > .attachment-content > .attachment-title").hasText("report.pdf");
    assertThat(rendered).element(".attachment > .attachment-actions > button.attachment-action").hasAttribute("aria-label");
    assertThat(rendered).element(".attachment > a.attachment-trigger:last-child").hasAttribute("href", "/files/1");
  }
}
