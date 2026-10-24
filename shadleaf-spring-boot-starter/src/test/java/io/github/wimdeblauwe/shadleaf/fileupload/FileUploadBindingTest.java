package io.github.wimdeblauwe.shadleaf.fileupload;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code sl:file-upload} in a {@code th:object} form: {@code th:field} names the hidden input and gives it its id, which
 * the Browse label points at, and a field with errors marks the input {@code aria-invalid}. In a {@code sl:field}, the
 * input takes the field's binding and {@code aria-describedby}.
 */
class FileUploadBindingTest {

  private final ComponentRenderTester renderer = ComponentRenderTester.create();

  @Test
  void theInputTakesItsIdAndNameFromTheField() {
    Rendered rendered = render(false, """
        <sl:file-upload th:field="*{attachments}" multiple/>""");

    Element input = rendered.select("input[type=file]").first();
    assertThat(input.id()).isEqualTo("attachments");
    assertThat(input.attr("name")).isEqualTo("attachments");
    assertThat(input.hasAttr("value")).isFalse();
    assertThat(input.hasAttr("aria-invalid")).isFalse();
    assertThat(rendered.select("label.file-upload-browse").attr("for")).isEqualTo("attachments");
  }

  @Test
  void aFieldWithErrorsMarksTheInputInvalid() {
    Rendered rendered = render(true, """
        <sl:file-upload th:field="*{attachments}" multiple/>""");

    assertThat(rendered.select("input[type=file]").attr("aria-invalid")).isEqualTo("true");
  }

  @Test
  void inAFieldTheInputTakesTheFieldsBindingAndDescriptions() {
    Rendered rendered = render(true, """
        <sl:field th:field="*{attachments}">
          <sl:field-label>Attachments</sl:field-label>
          <sl:file-upload multiple>
            <sl:slot name="description">PDF, up to 10 MB</sl:slot>
          </sl:file-upload>
          <sl:field-description>Shown to the whole team.</sl:field-description>
          <sl:field-error/>
        </sl:field>""");

    Element input = rendered.select("input[type=file]").first();
    assertThat(input.id()).isEqualTo("attachments");
    assertThat(input.attr("name")).isEqualTo("attachments");
    assertThat(input.attr("aria-invalid")).isEqualTo("true");
    assertThat(input.attr("aria-describedby"))
        .isEqualTo("attachments-upload-description attachments-description attachments-error");
    assertThat(rendered.select("label.field-label").attr("for")).isEqualTo("attachments");
    assertThat(rendered.select("label.file-upload-browse").attr("for")).isEqualTo("attachments");
    assertThat(rendered.select(".field-error").text()).isEqualTo("must hold at least one file");
  }

  private Rendered render(boolean invalid, String fields) {
    Upload upload = new Upload();
    BindingResult bindingResult = new BeanPropertyBindingResult(upload, "upload");
    if (invalid) {
      bindingResult.rejectValue("attachments", "NotEmpty", "must hold at least one file");
    }
    Map<String, Object> variables = new HashMap<>();
    variables.put("upload", upload);
    variables.put(BindingResult.MODEL_KEY_PREFIX + "upload", bindingResult);
    return renderer.render("<form th:object=\"${upload}\">" + fields + "</form>", variables);
  }

  /** A form backing object with JavaBeans accessors, as Spring's data binding expects. */
  public static class Upload {

    private List<MultipartFile> attachments = new ArrayList<>();

    public List<MultipartFile> getAttachments() {
      return attachments;
    }

    public void setAttachments(List<MultipartFile> attachments) {
      this.attachments = attachments;
    }
  }
}
