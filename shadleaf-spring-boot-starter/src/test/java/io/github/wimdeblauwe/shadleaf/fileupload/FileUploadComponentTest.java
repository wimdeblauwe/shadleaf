package io.github.wimdeblauwe.shadleaf.fileupload;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class FileUploadComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void rendersAVisuallyHiddenFileInputALabelAndAnEmptyList() {
    Rendered rendered = tester.render("<sl:file-upload name=\"files\"/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClassName("file-upload")
        .hasAttribute("x-data", "slFileUpload")
        .hasNoAttribute("data-max-size", "name");
    Element input = rendered.select(".file-upload > input").first();
    assertThat(input.attr("type")).isEqualTo("file");
    assertThat(input.classNames()).containsExactly("file-upload-input", "sl-sr-only");
    assertThat(input.attr("name")).isEqualTo("files");
    assertThat(input.id()).startsWith("sl-file-upload-");
    assertThat(input.hasAttr("multiple")).isFalse();
    assertThat(input.hasAttr("aria-describedby")).isFalse();

    Element label = rendered.select("label.file-upload-browse").first();
    assertThat(label.classNames()).containsExactly("button", "file-upload-browse");
    assertThat(label.attr("for")).isEqualTo(input.id());
    assertThat(label.text()).isEqualTo("Browse");
    assertThat(rendered.select(".empty-title").text()).isEqualTo("Upload a file");
    assertThat(rendered.select(".empty-description")).isEmpty();
    assertThat(rendered.select(".file-upload-list").first().hasAttr("hidden")).isTrue();
  }

  @Test
  void eachFileUploadHasItsOwnId() {
    Rendered rendered = tester.render("<sl:file-upload name=\"a\"/><sl:file-upload name=\"b\"/>");

    assertThat(rendered.select("input[type=file]")).extracting(Element::id).doesNotHaveDuplicates();
  }

  @Test
  void multipleTakesMoreFilesAndSaysSo() {
    Rendered rendered = tester.render("<sl:file-upload name=\"files\" multiple/>");

    assertThat(rendered.select("input[type=file]").first().hasAttr("multiple")).isTrue();
    assertThat(rendered.select(".empty-title").text()).isEqualTo("Upload files");
    assertThat(rendered.select("label.file-upload-browse").text()).isEqualTo("Browse files");
  }

  @Test
  void acceptGoesToTheInputAndMaxSizeToTheComponentInBytes() {
    Rendered rendered = tester.render("""
        <sl:file-upload name="files" accept="image/*,.pdf" max-size="10MB"/>""");

    assertThat(rendered).root().hasAttribute("data-max-size", "10485760");
    assertThat(rendered.select("input[type=file]").first().attr("accept")).isEqualTo("image/*,.pdf");
  }

  @Test
  void failsOnAMaxSizeThatIsNoSize() {
    assertThatRenderFailure(() -> tester.render("<sl:file-upload name=\"files\" max-size=\"ten megabytes\"/>"))
        .hasMessageContaining("max-size=\"ten megabytes\" is no size");
  }

  @Test
  void attributesGoToTheInputExceptTheClass() {
    Rendered rendered = tester.render("""
        <sl:file-upload id="photos" name="photos" class="mt-4" required hx-post="/upload"
                        aria-describedby="hint"/>""");

    assertThat(rendered).root().hasClass("file-upload", "mt-4").hasNoAttribute("id", "hx-post");
    Element input = rendered.select("input[type=file]").first();
    assertThat(input.id()).isEqualTo("photos");
    assertThat(input.hasAttr("required")).isTrue();
    assertThat(input.attr("hx-post")).isEqualTo("/upload");
    assertThat(input.attr("aria-describedby")).isEqualTo("hint");
    assertThat(rendered.select("label").attr("for")).isEqualTo("photos");
  }

  @Test
  void theDescriptionDescribesTheInput() {
    Rendered rendered = tester.render("""
        <sl:file-upload id="photos" name="photos" aria-describedby="hint">
          <sl:slot name="description">PNG or JPG, up to 5 MB</sl:slot>
        </sl:file-upload>""");

    Element description = rendered.select(".empty-description").first();
    assertThat(description.text()).isEqualTo("PNG or JPG, up to 5 MB");
    assertThat(description.id()).isEqualTo("photos-upload-description");
    assertThat(rendered.select("input[type=file]").attr("aria-describedby"))
        .isEqualTo("photos-upload-description hint");
  }

  @Test
  void theSlotsReplaceTheIconTitleAndBrowseText() {
    Rendered rendered = tester.render("""
        <sl:file-upload name="avatar" level="3" browse-label="Choose a photo">
          <sl:slot name="media"><sl:icon name="image"/></sl:slot>
          <sl:slot name="title">Profile photo</sl:slot>
        </sl:file-upload>""");

    assertThat(rendered.select(".empty-media svg").outerHtml())
        .isEqualTo(tester.render("<sl:icon name=\"image\"/>").root().outerHtml());
    Element title = rendered.select(".empty-title").first();
    assertThat(title.text()).isEqualTo("Profile photo");
    assertThat(title.attr("role")).isEqualTo("heading");
    assertThat(title.attr("aria-level")).isEqualTo("3");
    assertThat(rendered.select("label.file-upload-browse").text()).isEqualTo("Choose a photo");
  }

  @Test
  void theTitleIsNoHeadingWithoutALevel() {
    assertThat(tester.render("<sl:file-upload name=\"files\"/>").select(".empty-title").first().hasAttr("role"))
        .isFalse();
  }

  @Test
  void theTemplatesHoldAttachmentsWithMessagePatterns() {
    Rendered rendered = tester.render("<sl:file-upload name=\"files\" multiple/>");

    Element file = rendered.select("template[data-kind=file]").first();
    assertThat(file.select(".attachment").attr("data-state")).isEqualTo("idle");
    assertThat(file.select(".attachment").attr("data-size")).isEqualTo("sm");
    assertThat(file.select(".attachment-action").attr("aria-label")).isEqualTo("Remove {0}");

    Element rejected = rendered.select("template[data-kind=rejected]").first();
    assertThat(rejected.select(".attachment").attr("data-state")).isEqualTo("error");
    assertThat(rejected.select(".attachment-action").attr("aria-label")).isEqualTo("Dismiss {0}");
    assertThat(rejected.attr("data-message-type")).isEqualTo("File type not accepted");
    assertThat(rejected.attr("data-message-size")).isEqualTo("Larger than {0}");
    assertThat(rejected.attr("data-message-count")).isEqualTo("Only one file can be added");

    Element status = rendered.select(".file-upload-status").first();
    assertThat(status.attr("role")).isEqualTo("status");
    assertThat(status.text()).isEmpty();
    assertThat(status.attr("data-message-added")).isEqualTo("Added {0}.");
    assertThat(status.attr("data-message-removed")).isEqualTo("Removed {0}.");
    assertThat(status.attr("data-message-rejected")).isEqualTo("Could not add {0}: {1}.");
  }
}
