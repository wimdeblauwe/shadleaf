package io.github.wimdeblauwe.shadleaf.fileupload;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@code <sl:file-upload>}, rendered into {@code src/test/resources/approved/file-upload.approved.html}. */
class FileUploadApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void fileUpload() {
    HtmlApproval.verifyRenders("file-upload", tester, List.of(
        "<sl:file-upload id=\"avatar\" name=\"avatar\"/>",
        """
            <sl:file-upload id="files" name="files" multiple accept="image/png,image/jpeg,application/pdf"
                            max-size="10MB" level="3" class="max-w-md" previews>
              <sl:slot name="description">PNG, JPG, PDF up to 10 MB</sl:slot>
            </sl:file-upload>""",
        "<sl:file-upload id=\"list\" name=\"list\" multiple layout=\"list\"/>",
        "<sl:file-upload id=\"photos\" name=\"photos\" multiple accept=\"image/*\" layout=\"grid\" previews/>",
        """
            <sl:file-upload id="photo" name="photo" accept="image/*" browse-label="Choose a photo" disabled>
              <sl:slot name="media"><sl:icon name="image"/></sl:slot>
              <sl:slot name="title">Profile photo</sl:slot>
            </sl:file-upload>"""));
  }

  @Test
  void fileUploadInAFieldWithErrors() {
    FormModel form = FormModel.of(Map.of("files", List.of()), Map.of("files", List.of("Add at least one file")));
    HtmlApproval.verifyRenders("file-upload-field", tester, List.of(FormModel.wrap("""
        <sl:field th:field="*{files}">
          <sl:field-label>Attachments</sl:field-label>
          <sl:file-upload multiple/>
          <sl:field-error/>
        </sl:field>""")), form.variables());
  }
}
