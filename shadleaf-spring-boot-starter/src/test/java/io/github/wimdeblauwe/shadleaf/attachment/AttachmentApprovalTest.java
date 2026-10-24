package io.github.wimdeblauwe.shadleaf.attachment;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:attachment>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/attachment.approved.html}.
 */
class AttachmentApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void attachmentAndParts() {
    HtmlApproval.verifyRenders("attachment", tester, List.of(
        "<sl:attachment>Content</sl:attachment>",
        "<sl:attachment state=\"idle\">Content</sl:attachment>",
        "<sl:attachment state=\"uploading\">Content</sl:attachment>",
        "<sl:attachment state=\"processing\">Content</sl:attachment>",
        "<sl:attachment state=\"error\">Content</sl:attachment>",
        "<sl:attachment state=\"done\">Content</sl:attachment>",
        "<sl:attachment size=\"sm\">Content</sl:attachment>",
        "<sl:attachment size=\"xs\">Content</sl:attachment>",
        "<sl:attachment orientation=\"vertical\">Content</sl:attachment>",
        "<sl:attachment full-width>Content</sl:attachment>",
        "<sl:attachment-media><sl:icon name=\"file-text\"/></sl:attachment-media>",
        "<sl:attachment-media variant=\"image\"><img src=\"/files/1/thumbnail\" alt=\"\"></sl:attachment-media>",
        "<sl:attachment-content>Content</sl:attachment-content>",
        "<sl:attachment-title>report.pdf</sl:attachment-title>",
        "<sl:attachment-description>PDF · 2.4 MB</sl:attachment-description>",
        "<sl:attachment-actions>Actions</sl:attachment-actions>",
        "<sl:attachment-action aria-label=\"Remove report.pdf\"><sl:icon name=\"x\"/></sl:attachment-action>",
        """
            <sl:attachment-action as="a" href="/files/1" download size="icon-sm" variant="secondary"
                                  aria-label="Download report.pdf"><sl:icon name="download"/></sl:attachment-action>""",
        "<sl:attachment-trigger aria-label=\"Preview report.pdf\" commandfor=\"preview\" command=\"show-modal\"/>",
        "<sl:attachment-trigger as=\"a\" href=\"/files/1\" aria-label=\"Open report.pdf\"/>",
        "<sl:attachment-group>Attachments</sl:attachment-group>",
        "<sl:attachment-group label=\"Photos\">Attachments</sl:attachment-group>",
        "<sl:attachment-group layout=\"row\">Attachments</sl:attachment-group>",
        "<sl:attachment-group layout=\"grid\">Attachments</sl:attachment-group>",
        "<sl:attachment-group layout=\"grid\" label=\"Photos\">Attachments</sl:attachment-group>",
        """
            <sl:attachment state="error" id="upload-1">
              <sl:attachment-media><sl:icon name="file-warning"/></sl:attachment-media>
              <sl:attachment-content>
                <sl:attachment-title>financial-model.xlsx</sl:attachment-title>
                <sl:attachment-description>Upload failed. Try again.</sl:attachment-description>
              </sl:attachment-content>
              <sl:attachment-actions>
                <sl:attachment-action aria-label="Retry upload"><sl:icon name="refresh-cw"/></sl:attachment-action>
                <sl:attachment-action aria-label="Remove financial-model.xlsx"><sl:icon name="x"/></sl:attachment-action>
              </sl:attachment-actions>
              <sl:attachment-trigger as="a" href="/files/1" aria-label="Open financial-model.xlsx"/>
            </sl:attachment>""",
        """
            <sl:attachment th:state="${'uploading'}" th:size="${'sm'}">
              <sl:attachment-media><sl:spinner aria-hidden="true"/></sl:attachment-media>
              <sl:attachment-content>
                <sl:attachment-title>[[${'design-system.zip'}]]</sl:attachment-title>
              </sl:attachment-content>
            </sl:attachment>"""));
  }
}
